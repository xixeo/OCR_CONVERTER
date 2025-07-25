package org.test.ocrconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.test.ocrconverter.config.ConfigProperties;

@EnableConfigurationProperties(ConfigProperties.class)
@SpringBootApplication
public class OcrConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcrConverterApplication.class, args);
    }

}
