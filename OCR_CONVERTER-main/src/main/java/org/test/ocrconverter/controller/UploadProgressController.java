package org.test.ocrconverter.controller;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/upload/progress")
public class UploadProgressController {

    @Getter
    private static int totalFiles = 0;  // 총 파일 개수
    @Getter
    private static int processedFiles = 0;  // OCR 변환 완료된 파일 개수
    @Getter
    private static int pdfProgress = 0;  // PDF 변환 진행률

    public static void setTotalFiles(int total) {
        totalFiles = total;
        processedFiles = 0;  // OCR 시작 전 초기화
        pdfProgress = 0;  // PDF 변환 진행률 초기화
    }

    // PDF 변환 시작 알림
    public static void startPDFGeneration() {
        pdfProgress = 0;
    }

    // PDF 변환 완료 알림
    public static void completePDFGeneration() {
        pdfProgress = 100;
    }

    // OCR 및 PDF 변환 진행률 조회 API
    @GetMapping
    public ProgressResponse getProgress() {
        int ocrProgress = (totalFiles > 0) ? (processedFiles * 100 / totalFiles) : 0;
        int totalProgress = Math.round((ocrProgress * 0.8f) + (pdfProgress * 0.2f));
        return new ProgressResponse(processedFiles, totalFiles, ocrProgress, pdfProgress, totalProgress);
    }

    // 진행 상태 응답을 위한 내부 클래스
    @Getter
    public static class ProgressResponse {
        private final int processedFiles;
        private final int totalFiles;
        private final int ocrProgress;
        private final int pdfProgress;
        private final int totalProgress;

        public ProgressResponse(int processedFiles, int totalFiles, int ocrProgress, int pdfProgress, int totalProgress) {
            this.processedFiles = processedFiles;
            this.totalFiles = totalFiles;
            this.ocrProgress = ocrProgress;
            this.pdfProgress = pdfProgress;
            this.totalProgress = totalProgress;
        }

    }
}