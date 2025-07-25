package org.test.ocrconverter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.service.StatusTrackerService;
import org.test.ocrconverter.service.UploadStatusService;
import org.test.ocrconverter.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final UploadStatusService uploadStatusService;
    private final StatusTrackerService statusTrackerService;
    private final FileUtils fileUtils;
    private final ConfigProperties configProperties;

    @Autowired
    public ProgressController(UploadStatusService uploadStatusService, StatusTrackerService statusTrackerService, FileUtils fileUtils, ConfigProperties configProperties) {
        this.uploadStatusService = uploadStatusService;
        this.statusTrackerService = statusTrackerService;
        this.fileUtils = fileUtils;
        this.configProperties = configProperties;
    }

    // 업로드, 변환, 완료 파일 수 조회
    @GetMapping("/{uuid}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable String uuid, @RequestParam(required = false) String topFolderName) {
        Map<String, Object> progressData = new HashMap<>();

        int uploaded = statusTrackerService.getUploaded(uuid);
        int ocr = statusTrackerService.getOCRProcessed(uuid);
        int pdf = statusTrackerService.getPDFProcessed(uuid);
        String status = uploadStatusService.getStatus(uuid);
        int total = statusTrackerService.getTotalFiles(uuid);

        progressData.put("uploadedFiles", uploaded);
        progressData.put("ocrProcessedFiles", ocr);
        progressData.put("pdfProcessedFiles", pdf);
        progressData.put("totalFiles", total);
        progressData.put("status", status);

        if ("error".equals(status)) {
            log.warn("OCR 에러 상태 : {}", progressData);
            return ResponseEntity.ok(progressData);
        }

        if ("uploading".equals(status) || "processing".equals(status) || "generating_pdf".equals(status)) {
            log.info("📡 진행률 API : {}", progressData);
        }

        if ("generating_pdf".equals(status)) {
            boolean isPdfGenerated = statusTrackerService.isPDFGenerated(uuid);
            if (isPdfGenerated) {
                //log.info("PDF 변환 완료! 상태를 'completed'로 업데이트");
                uploadStatusService.saveStatus(uuid, "completed", total, total, new ArrayList<>(), topFolderName);
                progressData.put("status", "completed");
            }
        }

        return ResponseEntity.ok(progressData);
    }

    // OCR 재처리 가능 여부 반환
    @GetMapping("/check-restart-available/{uuid}")
    public ResponseEntity<Boolean> isRestartAvailable(@PathVariable String uuid) {
        Path userImageDir = Paths.get(configProperties.getImageDir(), uuid);
        File folder = userImageDir.toFile();

        List<File> imageFiles = fileUtils.findAllImages(folder);
        if (imageFiles.isEmpty()) {
            return ResponseEntity.ok(false);
        }

        String status = uploadStatusService.getStatus(uuid);
        boolean shouldShowRestart = !"completed".equals(status);
        return ResponseEntity.ok(shouldShowRestart);
    }
}
