package kz.cyber.acf.config;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorKz;
    private final String errorRu;
    private final String errorEn;

    public AppException(HttpStatus status, String errorKz, String errorRu, String errorEn) {
        super(errorEn);
        this.status = status;
        this.errorKz = errorKz;
        this.errorRu = errorRu;
        this.errorEn = errorEn;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorKz() { return errorKz; }
    public String getErrorRu() { return errorRu; }
    public String getErrorEn() { return errorEn; }
}
