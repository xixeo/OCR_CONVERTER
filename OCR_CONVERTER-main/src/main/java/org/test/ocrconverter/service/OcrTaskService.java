package org.test.ocrconverter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.test.ocrconverter.config.ConfigProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrTaskService {

    private final OcrServiceFactory ocrServiceFactory;
    private final ResultStorageService resultStorageService;
    private final FolderOCRService folderOCRService;
    private final ConfigProperties configProperties;
    private final StatusTrackerService statusTrackerService;

    @Async
    public void runOCRAsync(String uuid, String topFolderName, String ocrType, List<File> allImages) throws Exception {

        boolean isRestart = allImages.stream().anyMatch(file -> jsonAlreadyExists(file, uuid, topFolderName));
        if (isRestart) {
            log.info("OCR 재처리 시작 - UUID: {}, OCR: {}", uuid, ocrType);
        } else {
            log.info("OCR 처리 시작 - UUID: {}, OCR: {}", uuid, ocrType);
        }

        OcrService ocrService = ocrServiceFactory.getService(ocrType);

        int totalFiles = allImages.size();
        int alreadyDone = (int) allImages.stream()
                .filter(img -> jsonAlreadyExists(img, uuid, topFolderName))
                .count();
        int missingCount = totalFiles - alreadyDone;

        // 실제 스캔된 파일 수로 StatusTracker 업데이트
        statusTrackerService.init(uuid, totalFiles, topFolderName);
        statusTrackerService.markProcessing(uuid, topFolderName);

        int processed = 0;
        int failed = 0;

        List<String> logLines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTime = LocalDateTime.now().format(formatter);

        logLines.add("------------------------------------------------------------");
        logLines.add("[Start]: " + startTime);
        logLines.add("                                                            ");
        logLines.add("[No, 경로, 파일명, 결과, 오류메시지]");

        int index = 1;

        for (File imageFile : allImages) {
            String relativeFolder = getRelativeFolder(imageFile, uuid);
            String fileName = imageFile.getName();
            String result = "성공";
            String errorMessage = "";

            if (jsonAlreadyExists(imageFile, uuid, topFolderName)) {
                log.debug("이미 OCR 완료된 파일: {}", fileName);
                logLines.add(String.format("%d, %s, %s, %s, %s", index++, relativeFolder, fileName, result, errorMessage));
                statusTrackerService.incrementOCR(uuid);
                continue;
            }

            try {
                List<Map<String, Object>> ocrResults = ocrService.performOCR(imageFile);

                if (ocrResults != null && !ocrResults.isEmpty()) {
                    resultStorageService.save(imageFile.getAbsolutePath(), ocrResults, uuid, topFolderName);
                    statusTrackerService.incrementOCR(uuid);
                    processed++;
                    log.info("OCR 완료: {} (총 {}개 중 {}/{})", fileName, missingCount, processed, missingCount);
                    logLines.add(String.format("%d, %s, %s, %s, %s", index++, relativeFolder, fileName, "성공", ""));
                } else {
                    throw new IllegalStateException("OCR 결과 없음 또는 이미지 깨짐");
                }
            } catch (Exception e) {
                result = "실패";
                errorMessage = e.getMessage();
                failed++;

                log.warn("❌ OCR 실패 - 파일: {}, 이유: {}", fileName, errorMessage);
                logLines.add(String.format("%d, %s, %s, %s, %s", index++, relativeFolder, fileName, result, errorMessage == null ? "" : errorMessage));

                // OCR 실패해도 빈 JSON 파일 생성 (PDF에 이미지만 포함시키기 위해)
                try {
                    List<Map<String, Object>> emptyResults = new ArrayList<>();
                    resultStorageService.save(imageFile.getAbsolutePath(), emptyResults, uuid, topFolderName);
                    log.info("📄 OCR 실패한 파일에 대해 빈 JSON 생성: {}", fileName);
                } catch (Exception saveException) {
                    log.error("❌ 빈 JSON 파일 생성 실패: {}", fileName, saveException);
                }

                // 프로세스는 멈추지 않고 계속 진행
            }
        }

        String endTime = LocalDateTime.now().format(formatter);
        logLines.add("                                                            ");
        logLines.add("[End]: " + endTime);
        logLines.add(String.format("[요약] 총 파일 수: %d개, 성공: %d개, 실패: %d개", totalFiles, processed, failed));
        logLines.add("------------------------------------------------------------");

        // OCR 작업 로그 파일 저장
        try {
            Path logPath = Paths.get(configProperties.getOcrResultDir(), uuid, topFolderName, "ocr_log.txt");
            Files.createDirectories(logPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (String line : logLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            //log.info("📝 OCR 로그 저장 완료: {}", logPath);
        } catch (IOException e) {
            log.error("⚠️ OCR 로그 저장 실패", e);
        }

        // PDF 생성
        statusTrackerService.markGeneratingPDF(uuid, topFolderName);
        folderOCRService.convertJSONToPDF(uuid, topFolderName);

        // 완료 처리
        statusTrackerService.markCompleted(uuid, topFolderName);
        log.info("🎉 OCR + PDF 변환 완료 - UUID: {}", uuid);
    }

    private boolean jsonAlreadyExists(File imageFile, String uuid, String topFolderName) {
        try {
            Path originalPath = imageFile.toPath().toAbsolutePath();
            Path baseDir = Paths.get(configProperties.getImageDir(), uuid, topFolderName).toAbsolutePath();
            Path relativePath = originalPath.startsWith(baseDir) ? baseDir.relativize(originalPath) : originalPath.getFileName();

            Path resultDir = Paths.get(configProperties.getOcrResultDir(), uuid, topFolderName);
            if (relativePath.getParent() != null) {
                resultDir = resultDir.resolve(relativePath.getParent());
            }

            String imageName = imageFile.getName();
            String jsonName = imageName + ".json";
            Path jsonPath = resultDir.resolve(jsonName);
            return Files.exists(jsonPath);
        } catch (Exception e) {
            log.warn("JSON 파일 존재 여부 확인 실패: {}", imageFile.getAbsolutePath(), e);
            return false;
        }
    }

    private String getRelativeFolder(File imageFile, String uuid) {
        try {
            Path imagePath = imageFile.toPath().toAbsolutePath();
            Path basePath = Paths.get(configProperties.getImageDir(), uuid).toAbsolutePath();
            Path relativePath = basePath.relativize(imagePath).getParent();
            return relativePath != null ? relativePath.toString().replace("\\", "/") : "";
        } catch (Exception e) {
            log.warn("상대 경로 계산 실패: {}", imageFile.getAbsolutePath(), e);
            return "";
        }
    }

}
