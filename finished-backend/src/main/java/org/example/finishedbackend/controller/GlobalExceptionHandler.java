package org.example.finishedbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.RestBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public RestBean<Void> handleException(Exception e) {
        log.error("未处理的异常: ", e);
        return RestBean.failure(500, "服务器内部错误，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RestBean<Void> handleIllegalArgument(IllegalArgumentException e) {
        return RestBean.failure(400, e.getMessage());
    }
}
