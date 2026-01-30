package com.springleaf.thinkdo.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理类
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 JSON 请求参数校验异常 (@RequestBody @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常(JSON): {}", message);
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理表单参数绑定异常 (Form Data)
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常(Form): {}", message);
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理 @RequestParam @PathVariable 参数校验异常（如 @NotBlank, @Min 等）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束异常: {}", message);
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理 Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        return Result.error(ResultCodeEnum.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
    }

    /**
     * 捕获所有未处理的异常，返回统一的系统错误提示。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("未预期的异常", e);
        return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}