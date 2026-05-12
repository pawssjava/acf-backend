package kz.cyber.acf.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex) {
        int status = ex.getStatus().value();
        String error = ex.getStatus().getReasonPhrase();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status, error, ex.getErrorKz(), ex.getErrorRu(), ex.getErrorEn()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String error = HttpStatus.resolve(status) != null ? HttpStatus.resolve(status).getReasonPhrase() : "Error";
        String msg = ex.getReason();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status, error, msg, msg, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Техникалық мәселе. Қолдау қызметіне хабарласыңыз",
                        "Техническая проблема. Обратитесь в службу поддержки",
                        "Technical problem. Please contact support"));
    }
}
