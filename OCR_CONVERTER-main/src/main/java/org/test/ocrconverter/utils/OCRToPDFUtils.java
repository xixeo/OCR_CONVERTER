package org.test.ocrconverter.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.test.ocrconverter.config.ConfigProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class OCRToPDFUtils {

    private final ConfigProperties configProperties;

    @Autowired
    public OCRToPDFUtils(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public Map<File, List<File>> groupJsonFilesByFolder(File rootFolder) {
        Map<File, List<File>> folderMap = new HashMap<>();

        if (rootFolder.exists() && rootFolder.isDirectory()) {
            File[] files = rootFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderMap.putAll(groupJsonFilesByFolder(file));
                    } else if (file.getName().toLowerCase().endsWith(".json")) {
                        File parentFolder = file.getParentFile();
                        folderMap.computeIfAbsent(parentFolder, k -> new ArrayList<>()).add(file);
                    }
                }
            }
        }
        return folderMap;
    }

    // 하위 폴더 포함하여 JSON 파일 찾는 함수
    public List<File> findAllJsonFiles(File folder) {
        List<File> jsonFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        jsonFiles.addAll(findAllJsonFiles(file));
                    } else if (file.getName().toLowerCase().endsWith(".json")) {
                        jsonFiles.add(file);
                    }
                }
            }
        }
        jsonFiles.sort(Comparator.comparing(File::getName));
        return jsonFiles;
    }

    public PDType0Font loadFont(PDDocument document) throws Exception {
        InputStream fontStream = OCRToPDFUtils.class.getClassLoader().getResourceAsStream(configProperties.getFontResourcePath());
        if (fontStream == null) {
            log.warn("⚠️ 지정된 폰트 파일이 없습니다. 기본 폰트로 대체합니다.");
            File defaultFontFile = new File("C:/Windows/Fonts/malgun.ttf"); // 기본 한글 폰트 지정
            if (defaultFontFile.exists()) {
                return PDType0Font.load(document, defaultFontFile);
            } else {
                throw new IOException("🚨 기본 폰트 파일도 찾을 수 없습니다.");
            }
        }

        PDType0Font font = PDType0Font.load(document, fontStream, false);
        log.info("✅ PDF 폰트 로드 성공: {}", configProperties.getFontResourcePath());
        return font;
    }

    public void addImageAndTextToPDF(PDDocument document, File imageFile, JsonNode jsonNode, PDType0Font font) throws IOException {
        PDImageXObject image = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        
        // 🔥 원본 이미지 크기 그대로 PDF 페이지 생성
        PDRectangle customPageSize = new PDRectangle(imageWidth, imageHeight);
        PDPage page = new PDPage(customPageSize);
        document.addPage(page);
        
        // 📊 크기 정보 로그 출력
//        log.info("📐 이미지 파일: {} | 원본 크기: {}x{} pixels",
//                imageFile.getName(), (int)imageWidth, (int)imageHeight);
//        log.info("📄 PDF 페이지 크기: {}x{} points (1:1 매칭)",
//                (int)customPageSize.getWidth(), (int)customPageSize.getHeight());
        
        // 페이지 크기가 이미지 크기와 같으므로 스케일 1:1
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float pdfWidth = imageWidth;
        float pdfHeight = imageHeight;

        // 텍스트 그래픽 설정
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0f); // 텍스트를 투명하게 설정

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            // 이미지를 페이지 전체에 1:1 크기로 그리기
            contentStream.drawImage(image, 0, 0, imageWidth, imageHeight);
            contentStream.setFont(font, 12);
            contentStream.setGraphicsStateParameters(gs);

            for (int i = 1; i < jsonNode.size(); i++) {
                JsonNode node = jsonNode.get(i);
                if (node.has("boundingPoly") && node.get("boundingPoly").isArray() && node.get("boundingPoly").size() == 4) {
                    String originalText = node.path("text").asText("");

                    // 🚨 폰트에서 지원되는 문자만 필터링
                    StringBuilder filteredText = new StringBuilder();
                    for (char c : originalText.toCharArray()) {
                        try {
                            font.encode(Character.toString(c));
                            filteredText.append(c);
                        } catch (IllegalArgumentException e) {
                            log.warn("🚨 지원되지 않는 문자 제외: {}", c);
                        }
                    }

                    String text = filteredText.toString();
                    if (text.isEmpty()) continue;

                    JsonNode boundingPoly = node.get("boundingPoly");
                    int x1 = boundingPoly.get(0).get("x").asInt();
                    int y1 = boundingPoly.get(0).get("y").asInt();
                    int x2 = boundingPoly.get(2).get("x").asInt();
                    int y2 = boundingPoly.get(2).get("y").asInt();

                    // 🔥 기존 수식 그대로 사용
                    float pdfX = x1 * scaleX;
                    float fontSize = Math.max(Math.abs(y2 - y1) * scaleY, 10);
                    float pdfY = (float) (pdfHeight - ((y1 + (y2 - y1) / 2) * scaleY) - (fontSize / 2.2));
                    float textWidth = Math.abs(x2 - x1) * scaleX;
                    float scaleXFactor = textWidth / (font.getStringWidth(text) / 1000 * fontSize);

                    try {
                        font.encode(text);
                        contentStream.setFont(font, fontSize);
                        contentStream.beginText();
                        contentStream.setTextMatrix(new Matrix(scaleXFactor, 0, 0, 1, pdfX, pdfY));
                        contentStream.showText(text);
                        contentStream.endText();
                    } catch (IllegalArgumentException e) {
                        log.warn("🚨 지원되지 않는 전체 문자열 제외: {}", text);
                    }
                }
            }
        }
    }

    /**
     * OCR 결과가 없는 이미지만 PDF에 추가하는 메서드
     */
    public void addImageOnlyToPDF(PDDocument document, File imageFile) throws IOException {
        PDImageXObject image = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        
        // 원본 이미지 크기 그대로 PDF 페이지 생성
        PDRectangle customPageSize = new PDRectangle(imageWidth, imageHeight);
        PDPage page = new PDPage(customPageSize);
        document.addPage(page);
        
        log.info("📄 OCR 결과 없음 - 이미지만 PDF에 추가: {} ({}x{})", 
                imageFile.getName(), (int)imageWidth, (int)imageHeight);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // 이미지를 페이지 전체에 1:1 크기로 그리기
            contentStream.drawImage(image, 0, 0, imageWidth, imageHeight);
        }
    }

    /**
     * PDF 파일의 각 페이지 크기를 확인하는 메서드
     * @param pdfFile 확인할 PDF 파일
     * @return 페이지별 크기 정보
     */
    public void verifyPDFPageSizes(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            log.info("🔍 PDF 파일 페이지 크기 검증: {}", pdfFile.getName());
            log.info("📊 총 페이지 수: {}", document.getNumberOfPages());
            
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                log.info("📄 페이지 {}: {}x{} points", 
                        i + 1, (int)mediaBox.getWidth(), (int)mediaBox.getHeight());
            }
        } catch (IOException e) {
            log.error("❌ PDF 파일 로드 실패: {}", pdfFile.getName(), e);
        }
    }

    /**
     * 이미지 파일의 크기를 확인하는 메서드
     * @param imageFile 확인할 이미지 파일
     */
    public void verifyImageSize(File imageFile) {
        try (PDDocument tempDoc = new PDDocument()) {
            PDImageXObject image = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), tempDoc);
            log.info("🖼️ 이미지 파일: {} | 크기: {}x{} pixels", 
                    imageFile.getName(), (int)image.getWidth(), (int)image.getHeight());
        } catch (IOException e) {
            log.error("❌ 이미지 파일 로드 실패: {}", imageFile.getName(), e);
        }
    }
}
