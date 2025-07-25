package org.test.ocrconverter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcrServiceFactory {

    private final GoogleVisionOCRService googleVisionOCRService;
    private final PaddleOCRService paddleOCRService;

    public OcrService getService(String type) {
        return switch (type.toLowerCase()) {
            case "google" -> googleVisionOCRService;
            case "paddle" -> paddleOCRService;
            default -> throw new IllegalArgumentException("❌ 지원하지 않는 OCR 타입입니다: " + type);
        };
    }
}
