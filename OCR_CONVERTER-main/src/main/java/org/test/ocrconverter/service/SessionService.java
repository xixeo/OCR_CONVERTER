package org.test.ocrconverter.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SessionService {

    public ResponseEntity<String> getUUID(HttpSession session) {
        String uuid = (String) session.getAttribute("userUUID");
        if (uuid == null) {
            log.warn("❌ UUID가 세션에 없습니다!");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(uuid);
    }
}
