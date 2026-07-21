package kz.cyber.acf.document.service;

import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> TYPES = Set.of("consent", "privacy", "useragreement");
    private static final Set<String> LANGS = Set.of("ru", "en", "kz");

    private final MinioService minioService;

    public InputStream download(String type, String lang) {
        return minioService.download(objectName(type, lang));
    }

    public String objectName(String type, String lang) {
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
