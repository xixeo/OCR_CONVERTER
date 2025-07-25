package org.test.ocrconverter.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Slf4j
@Service
public class UploadPageService {

    public String loadUploadPage(Model model, HttpSession session) {
        Boolean showDownloadButton = (Boolean) session.getAttribute("showDownloadButton");
        model.addAttribute("showDownloadButton", showDownloadButton != null && showDownloadButton);
        log.info("📂 업로드 페이지 진입 - 다운로드 버튼 표시 여부: {}", showDownloadButton);
        return "upload";
    }
}
