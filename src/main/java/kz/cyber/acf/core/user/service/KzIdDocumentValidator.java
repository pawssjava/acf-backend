package kz.cyber.acf.core.user.service;

import kz.cyber.acf.config.AppException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KzIdDocumentValidator {

    private static final Pattern IIN_PATTERN = Pattern.compile("\\b(\\d{12})\\b");
    private static final Pattern VALIDITY_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s*-\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void validate(MultipartFile file) {
        byte[] bytes = readBytes(file);
        assertIsPdf(bytes);

        String text = extractText(bytes);

        assertHasIin(text);
        assertHasIssuingAuthority(text);
        assertHasMrz(text);
        assertNotExpired(text);
    }

    // — checks —

    private void assertIsPdf(byte[] bytes) {
        if (bytes.length < 5 || !new String(bytes, 0, 5).equals("%PDF-")) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Тек PDF файлдары қабылданады",
                    "Принимаются только PDF-файлы",
                    "Only PDF files are accepted");
        }
    }

    private void assertHasIin(String text) {
        if (!IIN_PATTERN.matcher(text).find()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Құжатта жарамды 12 таңбалы ЖСН жоқ",
                    "Документ не содержит корректный 12-значный ИИН",
                    "Document does not contain a valid 12-digit IIN");
        }
    }

    private void assertHasIssuingAuthority(String text) {
        if (!text.contains("ІШКІ ІСТЕР") && !text.contains("МВД") && !text.contains("МИНИСТРЛІГІ")) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Құжат Қазақстан Республикасының органдары берген жеке куәлік емес",
                    "Документ не является удостоверением личности, выданным органами Республики Казахстан",
                    "Document is not an identity card issued by the Republic of Kazakhstan");
        }
    }

    private void assertHasMrz(String text) {
        if (!text.contains("<<<<")) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Құжатта машина оқылатын аймақ (MRZ) жоқ",
                    "Документ не содержит машиночитаемую зону (MRZ)",
                    "Document does not contain a machine-readable zone (MRZ)");
        }
    }

    private void assertNotExpired(String text) {
        Matcher m = VALIDITY_PATTERN.matcher(text);
        if (!m.find()) return;
        try {
            LocalDate expiry = LocalDate.parse(m.group(2), DATE_FMT);
            if (expiry.isBefore(LocalDate.now())) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Құжаттың жарамдылық мерзімі өтіп кетті: " + m.group(2),
                        "Срок действия документа истёк: " + m.group(2),
                        "Document expired on: " + m.group(2));
            }
        } catch (DateTimeParseException ignored) {
        }
    }

    // — helpers —

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Жүктелген файлды оқу мүмкін болмады",
                    "Не удалось прочитать загруженный файл",
                    "Failed to read the uploaded file");
        }
    }

    private String extractText(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "PDF файлын өңдеу мүмкін болмады",
                    "Не удалось обработать PDF-файл",
                    "Failed to process the PDF file");
        }
    }
}
