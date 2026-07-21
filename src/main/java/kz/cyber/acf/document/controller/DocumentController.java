package kz.cyber.acf.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Documents", description = "Get a viewable link for legal documents (consent, privacy policy, user agreement)")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Get a presigned URL to view a legal document in the browser",
            description = "type: consent, privacy, useragreement. lang: ru, en, kz. " +
                    "The returned URL serves the PDF with Content-Type: application/pdf and " +
                    "Content-Disposition: inline, so opening it renders in the browser instead of downloading.")
    @GetMapping("/download")
    public Map<String, String> download(
            @RequestParam String type,
            @RequestParam String lang) {
        return Map.of("url", documentService.presignedUrl(type, lang));
    }
}
