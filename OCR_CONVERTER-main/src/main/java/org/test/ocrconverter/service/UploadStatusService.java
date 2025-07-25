package org.test.ocrconverter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.test.ocrconverter.config.ConfigProperties;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UploadStatusService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Integer> uploadedFilesMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> ocrProcessedFilesMap = new ConcurrentHashMap<>();
    private final ConfigProperties configProperties;

    public UploadStatusService(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    private String getStatusFilePath(String uuid) {
        return configProperties.getStatusDir() + "/" + uuid + "/status.json";
    }

    public synchronized void saveStatus(String uuid, String status, int processedFiles, int totalFiles,
                                        List<Map<String, String>> processedFileList, String topFolderName) {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("uuid", uuid);
        statusMap.put("status", status);
        statusMap.put("processedFiles", processedFiles);
        statusMap.put("totalFiles", totalFiles);

        int progress = (totalFiles > 0) ? (processedFiles * 100 / totalFiles) : 0;
        statusMap.put("progress", progress);
        statusMap.put("files", processedFileList);
        statusMap.put("topFolderName", topFolderName);

        File statusFile = new File(getStatusFilePath(uuid));
        File parentDir = statusFile.getParentFile();

        //log.info("저장할 경로: {}", statusFile.getAbsolutePath());

        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            //log.info("상위 디렉토리 생성: {} = {}", parentDir.getAbsolutePath(), created ? "성공" : "실패");
        }

        try {
            objectMapper.writeValue(statusFile, statusMap);
            //log.info("OCR 진행 상태 저장 완료: UUID={}, 상태={}, 진행률={}%", uuid, status, progress);
        } catch (IOException e) {
            log.error("OCR 상태 저장 실패: {}, 예외: {}", statusFile.getAbsolutePath(), e.getMessage());
        }
    }

    public void setUploadedFiles(String uuid, int count) {
        uploadedFilesMap.put(uuid, count);
    }

    public void setOCRProcessedFiles(String uuid, int count) {
        ocrProcessedFilesMap.put(uuid, count);
    }

    public synchronized void updateUploadedFiles(String uuid) {
        uploadedFilesMap.put(uuid, uploadedFilesMap.getOrDefault(uuid, 0) + 1);
    }

    public String getStatus(String uuid) {
        Map<String, Object> statusMap = loadStatus(uuid);
        return (String) statusMap.getOrDefault("status", "unknown");
    }


    public synchronized Map<String, Object> loadStatus(String uuid) {
        File file = new File(getStatusFilePath(uuid));
        if (!file.exists()) {
            log.warn("UUID={}의 OCR 상태 파일이 존재하지 않음", uuid);
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("OCR 상태 불러오기 실패", e);
            return new HashMap<>();
        }
    }

    public String findLastUUID() {
        File ocrResultsDir = new File(configProperties.getOcrResultDir());

        // OCR_RESULTS 폴더가 존재하는지 확인
        if (!ocrResultsDir.exists() || !ocrResultsDir.isDirectory()) {
            log.warn("OCR_RESULTS 폴더가 존재하지 않음.");
            return null;
        }

        // OCR_RESULTS 폴더 내부의 UUID 폴더 리스트 가져오기
        File[] uuidFolders = ocrResultsDir.listFiles(File::isDirectory);
        if (uuidFolders == null || uuidFolders.length == 0) {
            log.warn("OCR 기록이 없습니다.");
            return null;
        }

        // 가장 최근에 생성된 UUID 폴더 찾기
        File latestFolder = Arrays.stream(uuidFolders)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);

        log.info("복구된 마지막 UUID: {}", latestFolder.getName());
        return latestFolder.getName();

    }
}
