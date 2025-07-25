package org.test.ocrconverter.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.test.ocrconverter.service.UploadOrchestratorService;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/upload")
public class FolderUploadController {

    @Autowired private UploadOrchestratorService uploadOrchestratorService;

    @GetMapping("/get-uuid")
    public ResponseEntity<String> getUUID(HttpSession session) {
        return uploadOrchestratorService.getUUID(session);
    }

    @GetMapping("")
    public String uploadPage(Model model, HttpSession session) {
        return uploadOrchestratorService.loadUploadPage(model, session);
    }

    @PostMapping(value = "/restart-ocr", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> restartOCR(
            HttpSession session,
            @RequestParam(value = "ocrType", required = false, defaultValue = "paddle") String ocrType) throws Exception {
        return uploadOrchestratorService.restartOCR(session, ocrType);
    }

    @PostMapping("/folder")
    public ResponseEntity<Map<String, String>> uploadFolder(
            @RequestParam("images") MultipartFile[] files,
            @RequestParam("ocrType") String ocrType,
            @RequestParam("uuid") String uuid,
            @RequestParam("totalCount") int totalCount,
            HttpSession session
    ) throws Exception {
        return uploadOrchestratorService.handleFolderUpload(files, ocrType, uuid, totalCount, session);
    }
}
