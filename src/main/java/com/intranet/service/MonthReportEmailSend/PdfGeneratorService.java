package com.intranet.service.MonthReportEmailSend;

import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


@Service
public class PdfGeneratorService {

    public byte[] generatePdfFromHtml(String htmlContent) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();

        builder.withHtmlContent(htmlContent, null);
        builder.toStream(out);
        try {
            builder.run();
        } catch (IOException e) {
            
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generatePdfFromHtml2(String htmlContent) throws IOException {
        if (htmlContent == null) htmlContent = "";

        // 1) Trim stray whitespace / BOM
        htmlContent = htmlContent.trim();

        // 2) Basic sanity checks
        if (!htmlContent.startsWith("<") || !htmlContent.toLowerCase().contains("<html")) {
            throw new IOException("HTML content invalid or missing <html> root element.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        // baseUri = null is OK for inline content
        builder.withHtmlContent(htmlContent, null);
        builder.toStream(out);

        try {
            builder.run();
        } catch (Exception e) {
            // wrap for caller to see original cause
            throw new IOException("PDF generation failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }
}
