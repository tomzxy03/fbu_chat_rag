package com.tomzxy.fbu_chat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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

        // --- Auth errors ---

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), Instant.now()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse(401, "Unauthorized",
                                                "Sai tên đăng nhập hoặc mật khẩu", Instant.now()));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse(401, "Unauthorized",
                                                "Tài khoản đã bị vô hiệu hóa", Instant.now()));
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse(401, "Unauthorized",
                                                "Tài khoản đã bị khóa", Instant.now()));
        }

        // --- Validation & input errors ---

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

        @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
        public ResponseEntity<ErrorResponse> handleServiceUnavailable(
                        org.springframework.web.client.ResourceAccessException ex) {
                log.error("Service unreachable: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(new ErrorResponse(503, "Service Unavailable",
                                                "Không thể kết nối đến AI service. Vui lòng thử lại sau.",
                                                Instant.now()));
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
                log.error("Runtime exception: {}", ex.getMessage(), ex);
                // Một số RuntimeException có message thân thiện từ service layer — trả về trực tiếp
                // Các lỗi kỹ thuật (PGobject, Hibernate...) sẽ bị che bởi handleGeneric
                String msg = ex.getMessage();
                if (msg != null && msg.length() < 200 && !msg.contains("org.") && !msg.contains("com.")) {
                        return ResponseEntity.internalServerError()
                                        .body(new ErrorResponse(500, "Error", msg, Instant.now()));
                }
                return ResponseEntity.internalServerError()
                                .body(new ErrorResponse(500, "Error",
                                                "Đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.",
                                                Instant.now()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
                log.error("Unhandled exception: {}", ex.getMessage(), ex);
                // Không expose raw exception message ra ngoài — chỉ log nội bộ
                return ResponseEntity.internalServerError()
                                .body(new ErrorResponse(500, "Internal Error",
                                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", Instant.now()));
        }
}
