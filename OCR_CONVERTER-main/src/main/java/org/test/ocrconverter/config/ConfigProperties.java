package org.test.ocrconverter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ocr")
public class ConfigProperties {
    private String googleCredentialsPath;
    private String serverUrl;
    private String paddleUrl;
    private String resultDir;
    private String imageDir;
    private String outputPdfPath;
    private String ocrResultDir;
    private String fontResourcePath;
    private String statusDir;
}
