package com.example.watermark_app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/files")
public class FileController {

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("watermark") String watermarkText) {
        try {
            byte[] watermarkedFile = applyWatermark(file, watermarkText);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=watermarked.docx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(watermarkedFile);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private byte[] applyWatermark(MultipartFile file, String watermarkText) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Encrypt watermark
            String encryptedWatermark = AESUtil.encrypt(watermarkText);

            // Create or get existing header
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph para = header.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(encryptedWatermark);
            run.setColor("FFFFFF"); // white font (invisible)
            run.setFontSize(1); // tiny font
            run.setFontFamily("OCR A Extended");

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error applying watermark", e);
        }
    }

    private String extractWatermarkFromText(MultipartFile file) throws Exception {
        XWPFDocument document = new XWPFDocument(file.getInputStream());

        // 1. Check headers
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph para : header.getParagraphs()) {
                for (XWPFRun run : para.getRuns()) {
                    if ("FFFFFF".equals(run.getColor())) {
                        try {
                            return AESUtil.decrypt(run.text().trim());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // 2. Check footers
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph para : footer.getParagraphs()) {
                for (XWPFRun run : para.getRuns()) {
                    if ("FFFFFF".equals(run.getColor())) {
                        try {
                            return AESUtil.decrypt(run.text().trim());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // 3. Check body
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                if ("FFFFFF".equals(run.getColor())) {
                    try {
                        return AESUtil.decrypt(run.text().trim());
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw new Exception("No encrypted watermark found.");
    }

    @PostMapping("/decrypt")
    public ResponseEntity<String> decryptWatermark(@RequestParam("file") MultipartFile file) {
        try {
            String decrypted = extractWatermarkFromText(file);
            return ResponseEntity.ok(decrypted);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
