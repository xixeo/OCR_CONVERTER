package org.test.ocrconverter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.utils.FileUtils;
import org.test.ocrconverter.utils.ZipUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/download")
public class DownloadController {

    private final ConfigProperties configProperties;
    private final FileUtils fileUtils;

    @Autowired
    public DownloadController(ConfigProperties configProperties, FileUtils fileUtils) {
        this.configProperties = configProperties;
        this.fileUtils = fileUtils;
    }

    @GetMapping("/zip")
    public ResponseEntity<Resource> downloadZipWithJson(@RequestParam("uuid") String uuid) throws Exception {
        return createZipFile(uuid, true);
    }

    @GetMapping("/zip/pdf-only")
    public ResponseEntity<Resource> downloadZipWithoutJson(@RequestParam("uuid") String uuid) throws Exception {
        return createZipFile(uuid, false);
    }

    // ZIP 파일 생성 및 다운로드 후 폴더 삭제
    private ResponseEntity<Resource> createZipFile(String uuid, boolean includeJson) throws Exception {
        if (uuid == null || uuid.isEmpty()) {
            log.error("❌ UUID가 누락됨!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // ZIP 파일명 설정
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String zipFileName = "ocr_results_"+ timestamp + ".zip";
        String zipFilePath = configProperties.getOcrResultDir() + "/" + zipFileName;

        // 압축 대상 폴더 (UUID 포함)
        String sourceDir = configProperties.getOcrResultDir() + "/" + uuid;

        if (includeJson) {
            ZipUtils.zipDirectory(sourceDir, zipFilePath);
        } else {
            ZipUtils.zipOnlyPDFs(sourceDir, zipFilePath);
        }

        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 파일 다운로드 응답 생성
        Resource resource = new FileSystemResource(zipFile);
        ResponseEntity<Resource> response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                .body(resource);

        // 다운로드 완료 후 5초 후 UUID 폴더 및 압축 파일 삭제 (비동기 실행)
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 다운로드 완료 안정성 확보를 위해 대기
                deleteUUIDFiles(uuid, zipFilePath);
            } catch (InterruptedException e) {
                log.error("❌ 삭제 대기 중 오류 발생", e);
            }
        }).start();

        return response;

    }

    // 다운로드 완료 후 OCR 결과 폴더, 업로드 폴더, ZIP 파일 삭제
    private void deleteUUIDFiles(String uuid, String zipFilePath) {
        try {
            log.info("🗑️ 다운로드 완료! 관련 파일 삭제 시작...");

            // OCR 결과 폴더 삭제
            File ocrResultDir = new File(configProperties.getOcrResultDir(), uuid);
            if (ocrResultDir.exists()) {
                fileUtils.deleteDirectoryCompletely(ocrResultDir);
                log.info("🗑️ OCR 결과 폴더 삭제 완료: {}", ocrResultDir.getAbsolutePath());
            }

            // 업로드한 원본 이미지 폴더 삭제
            File imageUploadDir = new File(configProperties.getImageDir(), uuid);
            if (imageUploadDir.exists()) {
                fileUtils.deleteDirectoryCompletely(imageUploadDir);
                log.info("🗑️ 업로드된 원본 폴더 삭제 완료: {}", imageUploadDir.getAbsolutePath());
            }

            // 다운로드 후 생성된 ZIP 파일 삭제
            Files.deleteIfExists(Paths.get(zipFilePath));
            log.info("🗑️ ZIP 파일 삭제 완료: {}", zipFilePath);

            log.info("다운로드 후 모든 파일이 정상적으로 삭제되었습니다.");

        } catch (Exception e) {
            log.error("❌ UUID 파일 삭제 중 오류 발생", e);
        }
    }
}
