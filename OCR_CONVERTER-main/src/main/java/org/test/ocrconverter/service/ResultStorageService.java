// ResultStorageService.java
package org.test.ocrconverter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.utils.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultStorageService {

    private final ConfigProperties configProperties;
    private final FileUtils fileUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(String imageFilePath, List<Map<String, Object>> ocrResults, String uuid, String topFolderName) {
        try {
            Path originalPath = Paths.get(imageFilePath);
            Path baseDir = Paths.get(configProperties.getImageDir(), uuid, topFolderName).toAbsolutePath();
            Path relativePath = originalPath.startsWith(baseDir) ? baseDir.relativize(originalPath) : originalPath.getFileName();

            Path resultDir = Paths.get(configProperties.getOcrResultDir(), uuid, topFolderName);
            if (relativePath.getParent() != null) {
                resultDir = resultDir.resolve(relativePath.getParent());
            }

            fileUtils.ensureDirectoryExists(resultDir.toString());
            Path resultFile = resultDir.resolve(originalPath.getFileName().toString() + ".json");

            String json = objectMapper.writeValueAsString(ocrResults);
            Files.write(resultFile, json.getBytes());

            log.info("📄 OCR 결과 저장 완료: {}", resultFile);
        } catch (IOException e) {
            log.error("❌ OCR 결과 저장 실패: {}", imageFilePath, e);
        }
    }
}