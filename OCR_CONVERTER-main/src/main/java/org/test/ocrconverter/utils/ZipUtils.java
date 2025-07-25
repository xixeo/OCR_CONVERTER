package org.test.ocrconverter.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.InputStream;

public class ZipUtils {

    // 전체 폴더를 압축 (JSON 포함)
    public static void zipDirectory(String sourceDirPath, String zipFilePath) throws IOException {
        Path zipPath = Paths.get(zipFilePath);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Path sourcePath = Paths.get(sourceDirPath);
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            //  ocr_log.txt 포함 여부 확인
            Path logPath = sourcePath.resolve("ocr_log.txt");
            if (Files.exists(logPath)) {
                ZipEntry logEntry = new ZipEntry("ocr_log.txt");
                zos.putNextEntry(logEntry);
                try (InputStream logStream = Files.newInputStream(logPath)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = logStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    // PDF 파일만 압축
    public static void zipOnlyPDFs(String sourceDirPath, String zipFilePath) throws IOException {
        Path zipPath = Paths.get(zipFilePath);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Path sourcePath = Paths.get(sourceDirPath);
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".pdf"))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            Path logPath = sourcePath.resolve("ocr_log.txt");
            if (Files.exists(logPath)) {
                ZipEntry logEntry = new ZipEntry("ocr_log.txt");
                zos.putNextEntry(logEntry);
                try (InputStream logStream = Files.newInputStream(logPath)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = logStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }

    }
}
