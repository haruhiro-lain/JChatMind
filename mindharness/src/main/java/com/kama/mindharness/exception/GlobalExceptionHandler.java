package com.kama.mindharness.exception;

import com.kama.mindharness.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 捕获业务异常，错误信息返回给前端
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException e) {
        return ApiResponse.error(e.getMessage());
    }

    /**
     * 处理文件上传大小超限
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超过大小限制", e);
        return ApiResponse.error("文件过大，请确保文件大小不超过 50MB");
    }

    /**
     * 处理文件上传异常
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipartException(MultipartException e) {
        log.warn("文件上传异常", e);
        return ApiResponse.error("文件上传失败: " + e.getMessage());
    }

    /**
     * 处理 404 错误
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handle404(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 捕获所有未处理的异常, 对前端不返回错误信息
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ApiResponse.error("服务器内部错误");
    }
}
