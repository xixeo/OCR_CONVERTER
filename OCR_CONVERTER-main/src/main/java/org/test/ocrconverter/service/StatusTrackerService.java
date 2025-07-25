package org.test.ocrconverter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.test.ocrconverter.config.ConfigProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class StatusTrackerService {

    private final Map<String, Integer> uploadedFileCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> ocrProcessedCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> pdfProcessedCount = new ConcurrentHashMap<>();
    private final Map<String, String> currentStatus = new ConcurrentHashMap<>();
    private final Map<String, Integer> totalFileCount = new ConcurrentHashMap<>();

    @Autowired private UploadStatusService uploadStatusService;
    @Autowired private ConfigProperties configProperties;

    public void init(String uuid, int totalFiles, String topFolderName) {
        // 이미 초기화되어 있으면 totalFiles만 업데이트
        if (totalFileCount.containsKey(uuid)) {
            int currentTotal = totalFileCount.get(uuid);
            if (currentTotal != totalFiles) {
                totalFileCount.put(uuid, totalFiles);
                log.info("📊 UUID={} 총 파일 수 업데이트: {} -> {}", uuid, currentTotal, totalFiles);
            }
            return;
        }

        uploadedFileCount.put(uuid, 0);
        ocrProcessedCount.put(uuid, 0);
        pdfProcessedCount.put(uuid, 0);
        totalFileCount.put(uuid, totalFiles);
        currentStatus.put(uuid, "uploading");

        uploadStatusService.saveStatus(uuid, "uploading", 0, totalFiles, new ArrayList<>(), topFolderName);

        log.info("✅ UUID={} 상태 초기화 완료, 총 파일 수: {}", uuid, totalFiles);
    }

    public void incrementUploaded(String uuid) {
        uploadedFileCount.computeIfPresent(uuid, (k, v) -> v + 1);
        updateStatus(uuid);
    }

    public void incrementOCR(String uuid) {
        ocrProcessedCount.computeIfPresent(uuid, (k, v) -> v + 1);
        updateStatus(uuid);
    }

    public void incrementPDF(String uuid) {
        pdfProcessedCount.computeIfPresent(uuid, (k, v) -> v + 1);
        updateStatus(uuid);
    }

    public int getUploaded(String uuid) {
        return uploadedFileCount.getOrDefault(uuid, 0);
    }

    public int getOCRProcessed(String uuid) {
        return ocrProcessedCount.getOrDefault(uuid, 0);
    }

    public int getPDFProcessed(String uuid) {
        return pdfProcessedCount.getOrDefault(uuid, 0);
    }

    public int getTotalFiles(String uuid) {
        return totalFileCount.getOrDefault(uuid, 0);
    }

    public boolean isPDFGenerated(String uuid) {
        Path pdfPath = Paths.get(configProperties.getOcrResultDir(), uuid, "OCR_Result.pdf");
        return Files.exists(pdfPath);
    }

    public void updateStatus(String uuid) {
        String status = currentStatus.getOrDefault(uuid, "processing");
        int uploaded = getUploaded(uuid);
        int ocr = getOCRProcessed(uuid);
        int total = totalFileCount.getOrDefault(uuid, 1);

        uploadStatusService.setUploadedFiles(uuid, uploaded);
        uploadStatusService.setOCRProcessedFiles(uuid, ocr);

        log.debug("📊 상태 업데이트 - UUID: {}, 상태: {}, 업로드: {}/{}, OCR: {}/{}",
                uuid, status, uploaded, total, ocr, total);
    }

    public void markCompleted(String uuid, String topFolderName) {
        int total = totalFileCount.getOrDefault(uuid, 1);
        currentStatus.put(uuid, "completed");
        uploadStatusService.saveStatus(uuid, "completed", total, total, new ArrayList<>(), topFolderName);
    }

    public void markGeneratingPDF(String uuid, String topFolderName) {
        int total = totalFileCount.getOrDefault(uuid, 1);
        currentStatus.put(uuid, "generating_pdf");
        uploadStatusService.saveStatus(uuid, "generating_pdf", 0, total, new ArrayList<>(), topFolderName);
    }

    public void markProcessing(String uuid, String topFolderName) {
        int total = totalFileCount.getOrDefault(uuid, 1);
        currentStatus.put(uuid, "processing");
        uploadStatusService.saveStatus(uuid, "processing", 0, total, new ArrayList<>(), topFolderName);
    }
}
