package org.test.ocrconverter.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileUploadService {

    @Autowired private ConfigProperties configProperties;
    @Autowired private FileUtils fileUtils;
    @Autowired private UploadStatusService uploadStatusService;
    @Autowired private StatusTrackerService statusTrackerService;
    @Autowired private OcrTaskService ocrTaskService;

    public ResponseEntity<Map<String, String>> handleFolderUpload(MultipartFile[] files, String ocrType, String uuid, int totalCount, HttpSession session) throws Exception {
        session.setAttribute("userUUID", uuid);
        log.info("📥 업로드 요청 수신 - UUID: {}, 총 파일 수: {}", uuid, totalCount);

        if (files.length == 0 || files[0].getOriginalFilename() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "업로드된 파일이 없습니다."));

        String rootFolderName = files[0].getOriginalFilename().split("/")[0];
        Map<String, String> logMap = new ConcurrentHashMap<>();

        statusTrackerService.init(uuid, totalCount, rootFolderName);

        List<File> savedFiles = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String relativePath = file.getOriginalFilename();
                if (relativePath == null || relativePath.trim().isEmpty()) continue;

                File destination = Paths.get(configProperties.getImageDir(), uuid, relativePath).toFile();
                if (destination.getParentFile().mkdirs())
                    log.info("디렉토리 생성 성공: {}", destination.getParent());

                file.transferTo(destination);
                savedFiles.add(destination);
                statusTrackerService.incrementUploaded(uuid);
                logMap.put(relativePath.replace("\\", "/"), "upload=ok, ocr=pending");
            }
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "파일 저장 중 오류 발생"));
        }

        if (statusTrackerService.getUploaded(uuid) >= totalCount) {
            statusTrackerService.markProcessing(uuid, rootFolderName);
            
            // 저장된 폴더에서 모든 이미지 파일 스캔 (하위 폴더 포함)
            File uploadedFolder = Paths.get(configProperties.getImageDir(), uuid, rootFolderName).toFile();
            List<File> allImageFiles = fileUtils.findAllImages(uploadedFolder);
            
            log.info("📁 전체 이미지 파일 스캔 완료 - 총 {}개 파일 발견", allImageFiles.size());
            
            ocrTaskService.runOCRAsync(uuid, rootFolderName, ocrType, allImageFiles);
        }

        return ResponseEntity.ok(Map.of("uuid", uuid));
    }
}
