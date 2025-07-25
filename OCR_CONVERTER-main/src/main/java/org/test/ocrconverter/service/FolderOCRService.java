package org.test.ocrconverter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.test.ocrconverter.config.ConfigProperties;
import org.test.ocrconverter.utils.FileUtils;
import org.test.ocrconverter.utils.OCRToPDFUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderOCRService {

    private final ConfigProperties configProperties;
    private final FileUtils fileUtils;
    private final OCRToPDFUtils ocrToPDFUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StatusTrackerService statusTrackerService;

    public void convertJSONToPDF(String uuid, String topFolderName) throws Exception {
        try {
            File resultFolder = Paths.get(configProperties.getOcrResultDir(), uuid, topFolderName).toFile();
            Map<File, List<File>> folderMap = ocrToPDFUtils.groupJsonFilesByFolder(resultFolder);

            for (Map.Entry<File, List<File>> entry : folderMap.entrySet()) {
                File folder = entry.getKey();
                List<File> jsonFiles = entry.getValue();
                if (jsonFiles.isEmpty()) continue;

                // 🔽 파일명 오름차순 정렬
                jsonFiles.sort(Comparator.comparing(File::getName));

                PDDocument document = new PDDocument();
                PDType0Font font = ocrToPDFUtils.loadFont(document);

                for (File jsonFile : jsonFiles) {
                    String relativePath = resultFolder.toPath().relativize(jsonFile.toPath()).toString().replace(".json", "");
                    File imageFile = new File(Paths.get(configProperties.getImageDir(), uuid, topFolderName, relativePath).toString());

                    if (!imageFile.exists()) {
                        log.warn("⚠️ 이미지 파일이 존재하지 않아 PDF에 포함되지 않음: {}", imageFile);
                        continue;
                    }

                    JsonNode jsonNode = objectMapper.readTree(jsonFile);
                    
                    // JSON이 비어있어도 이미지는 PDF에 포함
                    if (jsonNode == null || !jsonNode.isArray() || jsonNode.size() == 0) {
                        log.info("📄 OCR 결과가 없는 이미지를 PDF에 포함: {}", imageFile.getName());
                        ocrToPDFUtils.addImageOnlyToPDF(document, imageFile);
                    } else {
                        ocrToPDFUtils.addImageAndTextToPDF(document, imageFile, jsonNode, font);
                    }

                    statusTrackerService.incrementPDF(uuid);
                }


                try {
                    String pdfName = folder.getName() + ".pdf";
                    fileUtils.ensureDirectoryExists(folder.getAbsolutePath());
                    File outputPDF = new File(folder, pdfName);
                    document.save(outputPDF);
                    log.info("✅ PDF 저장 완료: {}", outputPDF.getAbsolutePath());
                } finally {
                    document.close();
                }
            }

        } catch (Exception e) {
            log.error("❌ 전체 PDF 변환 중 오류 발생", e);
        }
    }
}