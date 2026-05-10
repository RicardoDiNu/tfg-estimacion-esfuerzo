package com.uniovi.estimacion.services.reports;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfReportRendererService {

    private final TemplateEngine templateEngine;

    public byte[] renderToPdf(String templateName, Map<String, Object> model) {
        Locale locale = LocaleContextHolder.getLocale();
        Context context = new Context(locale);

        if (model != null) {
            model.forEach(context::setVariable);
        }

        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el informe PDF.", exception);
        }
    }
}