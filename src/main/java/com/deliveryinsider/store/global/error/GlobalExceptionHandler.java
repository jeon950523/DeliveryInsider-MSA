package com.deliveryinsider.store.global.error;

import java.util.LinkedHashMap;
import java.util.Map;

import com.deliveryinsider.store.global.response.GlobalResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode c=e.errorCode(); return ResponseEntity.status(c.status()).body(GlobalResponse.error(c.code(),c.message(),null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Map<String,String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String,String> errors=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x->errors.putIfAbsent(x.getField(),x.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors().forEach(x->errors.putIfAbsent("request",x.getDefaultMessage()));
        ErrorCode c=CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.status()).body(GlobalResponse.error(c.code(),c.message(),errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        ErrorCode c=CommonErrorCode.MALFORMED_JSON; return ResponseEntity.status(c.status()).body(GlobalResponse.error(c.code(),c.message(),null));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    public ResponseEntity<GlobalResponse<Void>> handleTypeMismatch(Exception e) {
        ErrorCode c=CommonErrorCode.TYPE_MISMATCH; return ResponseEntity.status(c.status()).body(GlobalResponse.error(c.code(),c.message(),null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected server error",e); ErrorCode c=CommonErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(c.status()).body(GlobalResponse.error(c.code(),c.message(),null));
    }
}
