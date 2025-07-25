package org.test.ocrconverter.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
public class UploadOrchestratorService {

    @Autowired private SessionService sessionService;
    @Autowired private UploadPageService uploadPageService;
    @Autowired private OCRRestartService ocrRestartService;
    @Autowired private FileUploadService fileUploadService;

    public ResponseEntity<String> getUUID(HttpSession session) {
        return sessionService.getUUID(session);
    }

    public String loadUploadPage(Model model, HttpSession session) {
        return uploadPageService.loadUploadPage(model, session);
    }

    public ResponseEntity<Map<String, Object>> restartOCR(HttpSession session, String ocrType) throws Exception {
        return ocrRestartService.restartOCR(session, ocrType);
    }

    public ResponseEntity<Map<String, String>> handleFolderUpload(MultipartFile[] files, String ocrType, String uuid, int totalCount, HttpSession session) throws Exception {
        return fileUploadService.handleFolderUpload(files, ocrType, uuid, totalCount, session);
    }
}
