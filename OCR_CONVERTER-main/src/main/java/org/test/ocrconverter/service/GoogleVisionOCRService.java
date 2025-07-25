// GoogleVisionOCRService.java
package org.test.ocrconverter.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleVisionOCRService implements OcrService {

    @Override
    public List<Map<String, Object>> performOCR(File imageFile) {
        return performOCRBatch(Collections.singletonList(imageFile));
    }

    // 이미지 변환 요청
    public List<Map<String, Object>> performOCRBatch(List<File> files) {
        try {
            List<AnnotateImageRequest> requests = files.stream().map(file -> {
                try {
                    ByteString imgBytes = ByteString.copyFrom(Files.readAllBytes(file.toPath()));
                    Image img = Image.newBuilder().setContent(imgBytes).build();

                    Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
                    return AnnotateImageRequest.newBuilder()
                            .addFeatures(feat)
                            .setImage(img)
                            .build();
                } catch (IOException e) {
                    log.error("❌ OCR 변환 실패: {}", file.getName(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (requests.isEmpty()) {
                log.warn("⚠️ OCR 요청할 이미지가 없습니다.");
                return Collections.emptyList();
            }

            // Google Vision API 호출
            List<AnnotateImageResponse> responses = callGoogleVisionAPI(requests);

            return parseOCRResults(responses);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("❌ Batch OCR 처리 실패", e);
            }
            return Collections.emptyList();
        }
    }

    // Google Vision API 호출 메서드 (공통 사용)
    private List<AnnotateImageResponse> callGoogleVisionAPI(List<AnnotateImageRequest> requests) {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            return response.getResponsesList();
        } catch (IOException e) {
            log.error("❌ Google Vision API 호출 실패", e);
            return Collections.emptyList();
        }
    }

    // OCR 결과 JSON으로 변환 (공통 사용)
    private List<Map<String, Object>> parseOCRResults(List<AnnotateImageResponse> responses) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                log.error("🚨 OCR 오류 발생: {}", res.getError().getMessage());
                continue;
            }

            for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", annotation.getDescription());

                // BoundingPoly 데이터 변환 (순환 참조 방지)
                if (annotation.hasBoundingPoly()) {
                    List<Map<String, Integer>> boundingBox = annotation.getBoundingPoly().getVerticesList().stream()
                            .map(vertex -> {
                                Map<String, Integer> point = new HashMap<>();
                                point.put("x", vertex.getX());
                                point.put("y", vertex.getY());
                                return point;
                            })
                            .collect(Collectors.toList());
                    data.put("boundingPoly", boundingBox);
                }

                results.add(data);
            }
        }
        return results;
    }
}
