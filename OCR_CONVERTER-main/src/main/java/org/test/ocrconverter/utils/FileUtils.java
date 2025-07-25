package org.test.ocrconverter.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.test.ocrconverter.config.ConfigProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
@Component
public class FileUtils {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");
    private final ConfigProperties configProperties;

    @Autowired
    public FileUtils(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    // 특정 폴더 내 모든 이미지 파일 찾기
    public List<File> findAllImages(File folder) {
        List<File> imageFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        imageFiles.addAll(findAllImages(file));
                    } else if (isImageFile(file)) imageFiles.add(file);
                }
            }
        }
        return imageFiles;
    }

    // 특정 폴더 및 하위 폴더 내 모든 JSON 파일 찾기
    public List<File> findAllJsonFiles(File folder) {
        List<File> jsonFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        jsonFiles.addAll(findAllJsonFiles(file)); // 재귀 탐색
                    } else if (file.getName().toLowerCase().endsWith(".json")) {
                        jsonFiles.add(file);
                    }
                }
            }
        }
        return jsonFiles;
    }

    // File 객체를 검사하는 메서드
    public boolean isImageFile(File file) {
        return file != null && isImageFile(file.getName());
    }

    // 파일명에서 확장자를 확인하는 메서드
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lowerCaseName = fileName.toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(lowerCaseName::endsWith);
    }

    // 폴더가 없으면 생성
    public void ensureDirectoryExists(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            synchronized (FileUtils.class) {
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (created) {
                        log.info("경로 생성 완료: {}", directoryPath);
                    } else {
                        log.error("폴더 생성 실패 (접근 권한 문제): {}", directoryPath);
                    }
                }
            }
        }
    }

    // 특정 폴더 및 내부 파일 삭제
    public void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file); // 재귀 삭제
                    } else {
                        if (file.delete()) {
                            log.info("📄 파일 삭제 완료: {}", file.getAbsolutePath());
                        } else {
                            log.warn("⚠️ 파일 삭제 실패: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
            
            // 🔥 폴더 자체도 삭제 (모든 내용물 삭제 후)
            if (directory.delete()) {
                log.info("📁 폴더 삭제 완료: {}", directory.getAbsolutePath());
            } else {
                log.warn("⚠️ 폴더 삭제 실패: {}", directory.getAbsolutePath());
            }
        } else {
            log.warn("⚠️ 삭제하려는 폴더가 존재하지 않음: {}", directory.getAbsolutePath());
        }
    }

    /**
     * 더 안전한 폴더 삭제 메서드 (Java NIO 사용)
     * 폴더와 모든 내용을 완전히 삭제합니다.
     */
    public void deleteDirectoryCompletely(File directory) {
        if (!directory.exists()) {
            log.warn("⚠️ 삭제하려는 폴더가 존재하지 않음: {}", directory.getAbsolutePath());
            return;
        }
        
        try {
            Path dirPath = directory.toPath();
            log.info("🗑️ 폴더 완전 삭제 시작: {}", dirPath);
            
            // 폴더 내 모든 파일과 하위 폴더를 역순으로 삭제
            Files.walk(dirPath)
                    .sorted(Comparator.reverseOrder()) // 깊은 곳부터 삭제 (파일 -> 폴더)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("🗑️ 삭제 완료: {}", path);
                        } catch (IOException e) {
                            log.error("❌ 삭제 실패: {} - {}", path, e.getMessage());
                        }
                    });
                    
            // 최종 확인
            if (!directory.exists()) {
                log.info("✅ 폴더 완전 삭제 성공: {}", dirPath);
            } else {
                log.warn("⚠️ 폴더가 아직 남아있음: {}", dirPath);
            }
            
        } catch (IOException e) {
            log.error("❌ 폴더 삭제 중 오류 발생: {}", directory.getAbsolutePath(), e);
            // 실패 시 기존 방식으로 재시도
            log.info("🔄 기존 방식으로 재시도...");
            deleteDirectory(directory);
        }
    }
}
