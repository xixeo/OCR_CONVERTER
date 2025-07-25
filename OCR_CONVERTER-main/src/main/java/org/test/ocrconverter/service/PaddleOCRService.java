// 🔁 PaddleOCRService.java (수정됨): 여러 이미지 한 번에 보내는 구조
package org.test.ocrconverter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.test.ocrconverter.config.ConfigProperties;

import java.io.File;
import java.util.*;

@Slf4j
@Service
public class PaddleOCRService implements OcrService {

    private final String paddleOcrApiUrl;

    @Autowired
    public PaddleOCRService(ConfigProperties configProperties) {
        this.paddleOcrApiUrl = configProperties.getPaddleUrl();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 단일 이미지 OCR
    @Override
    public List<Map<String, Object>> performOCR(File imageFile) throws Exception {
        List<Map<String, Object>> result = performBatchOCR(List.of(imageFile)).getOrDefault(imageFile.getName(), Collections.emptyList());
        return result;
    }

    // 다중 이미지 병렬 OCR 요청
    public Map<String, List<Map<String, Object>>> performBatchOCR(List<File> imageFiles) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (File imageFile : imageFiles) {
            body.add("images", new FileSystemResource(imageFile));
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                paddleOcrApiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } else {
            log.warn("⚠️ PaddleOCR 응답 상태 이상: {}", response.getStatusCode());
        }

        return new HashMap<>();
    }
}
