package org.test.ocrconverter.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class OCRRestartService {

    @Autowired private ConfigProperties configProperties;
    @Autowired private FileUtils fileUtils;
    @Autowired private UploadStatusService uploadStatusService;
    @Autowired private StatusTrackerService statusTrackerService;
    @Autowired private OcrTaskService ocrTaskService;

    public ResponseEntity<Map<String, Object>> restartOCR(HttpSession session, String ocrType) throws Exception {
        Map<String, Object> response = new HashMap<>();
        String uuid = (String) session.getAttribute("userUUID");

        if (uuid == null || uuid.isEmpty()) {
            uuid = uploadStatusService.findLastUUID();
            if (uuid == null) {
                response.put("success", false);
                response.put("message", "❌ UUID 또는 업로드 기록을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            session.setAttribute("userUUID", uuid);
        }

        String topFolderName = findTopFolderName(uuid);
        if (topFolderName == null) {
            response.put("success", false);
            response.put("message", "❌ topFolderName을 복구할 수 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        Path imageDirPath = Paths.get(configProperties.getImageDir(), uuid, topFolderName);
        Path jsonDirPath = Paths.get(configProperties.getOcrResultDir(), uuid, topFolderName);

        List<File> allImages = fileUtils.findAllImages(imageDirPath.toFile());
        List<File> jsonFiles = fileUtils.findAllJsonFiles(jsonDirPath.toFile());

        int totalCount = allImages.size();
        statusTrackerService.init(uuid, totalCount, topFolderName);
        statusTrackerService.markProcessing(uuid, topFolderName);

        ocrTaskService.runOCRAsync(uuid, topFolderName, ocrType, allImages);

        response.put("success", true);
        response.put("uuid", uuid);
        response.put("showDownloadButton", false);

        return ResponseEntity.ok(response);
    }

    private String findTopFolderName(String uuid) {
        File imageRoot = Paths.get(configProperties.getImageDir(), uuid).toFile();
        File[] subDirs = imageRoot.listFiles(File::isDirectory);
        if (subDirs != null && subDirs.length > 0) {
            return subDirs[0].getName();
        }
        return null;
    }
}
