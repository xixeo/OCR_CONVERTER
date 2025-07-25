package org.test.ocrconverter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.test.ocrconverter.utils.OCRToPDFUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class DirectoryController {

    @Autowired
    private OCRToPDFUtils ocrToPDFUtils;


    @GetMapping("/directory/list")
    public List<Map<String, String>> getDirectoryList(@RequestParam String path) {

        String normalizedPath = Paths.get(path).normalize().toString();
        log.info("📂 디렉토리 조회 요청: {}", normalizedPath);

        File folder = new File(normalizedPath);
        List<Map<String, String>> items = new ArrayList<>();

        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("🚨 유효하지 않은 경로: {}", normalizedPath);
            return items;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                Map<String, String> item = new HashMap<>();
                item.put("name", file.getName());
                item.put("path", file.getAbsolutePath().replace("\\", "/"));
                item.put("type", file.isDirectory() ? "directory" : "file");
                items.add(item);
            }
        }
        return items;
    }

    /**
     * PDF 파일과 원본 이미지 파일의 크기를 비교 검증하는 엔드포인트
     * @param pdfPath PDF 파일 경로
     * @param imagePath 이미지 파일 경로 (선택사항)
     * @return 크기 비교 결과
     */
    @GetMapping("/verify/size")
    public Map<String, Object> verifySizeComparison(
            @RequestParam String pdfPath,
            @RequestParam(required = false) String imagePath) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            File pdfFile = new File(pdfPath);
            
            if (!pdfFile.exists()) {
                result.put("error", "PDF 파일을 찾을 수 없습니다: " + pdfPath);
                return result;
            }
            
            log.info("🔍 크기 검증 시작 - PDF: {}", pdfFile.getName());
            
            // PDF 크기 확인
            ocrToPDFUtils.verifyPDFPageSizes(pdfFile);
            result.put("pdfFile", pdfFile.getName());
            result.put("pdfPath", pdfPath);
            
            // 이미지 파일이 지정된 경우 비교
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    log.info("🖼️ 원본 이미지 크기 확인");
                    ocrToPDFUtils.verifyImageSize(imageFile);
                    result.put("imageFile", imageFile.getName());
                    result.put("imagePath", imagePath);
                } else {
                    result.put("imageWarning", "이미지 파일을 찾을 수 없습니다: " + imagePath);
                }
            }
            
            result.put("status", "success");
            result.put("message", "크기 검증이 완료되었습니다. 로그를 확인하세요.");
            
        } catch (Exception e) {
            log.error("❌ 크기 검증 중 오류 발생", e);
            result.put("error", "크기 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

}
