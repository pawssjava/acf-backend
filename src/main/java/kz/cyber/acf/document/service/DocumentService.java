package kz.cyber.acf.document.service;

import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> TYPES = Set.of("consent", "privacy", "useragreement");
    private static final Set<String> LANGS = Set.of("ru", "en", "kz");
    private static final int EXPIRY_HOURS = 1;

    private final MinioService minioService;

    public String presignedUrl(String type, String lang) {
        String objectName = objectName(type, lang);
        Map<String, String> responseHeaders = Map.of(
                "response-content-type", "application/pdf",
                "response-content-disposition", "inline; filename=\"" + objectName + "\""
        );
        return minioService.presignedUrl(objectName, EXPIRY_HOURS, responseHeaders);
    }

    private String objectName(String type, String lang) {
        String normalizedType = type.toLowerCase();
        String normalizedLang = lang.toLowerCase();
        if (!TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown document type: " + type);
        }
        if (!LANGS.contains(normalizedLang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language: " + lang);
        }
        return normalizedType + "_" + normalizedLang + ".pdf";
    }
}
