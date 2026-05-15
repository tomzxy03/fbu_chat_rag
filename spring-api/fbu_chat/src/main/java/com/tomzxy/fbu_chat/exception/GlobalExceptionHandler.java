package com.tomzxy.fbu_chat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        record ErrorResponse(int status, String error, Object message, Instant timestamp) {
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(400, "Validation Error", errors, Instant.now()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), Instant.now()));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleFileSizeExceeded(MaxUploadSizeExceededException ex) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                                .body(new ErrorResponse(413, "File Too Large", "File vượt quá giới hạn 50MB",
                                                Instant.now()));
        }

        @ExceptionHandler(HttpClientErrorException.class)
        public ResponseEntity<ErrorResponse> handleAiServiceError(HttpClientErrorException ex) {
                log.error("AI Service error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(new ErrorResponse(502, "AI Service Error",
                                                "AI service trả về lỗi: " + ex.getStatusCode(), Instant.now()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
                log.error("Unhandled exception", ex);
                return ResponseEntity.internalServerError()
                                .body(new ErrorResponse(500, "Internal Error", ex.getMessage(), Instant.now()));
        }
}
