package org.test.ocrconverter.service;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface OcrService {
    List<Map<String, Object>> performOCR(File imageFile) throws Exception;
}