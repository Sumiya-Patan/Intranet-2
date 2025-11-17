package com.intranet.service.MonthReportEmailSend;

import org.springframework.stereotype.Service;
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
}
