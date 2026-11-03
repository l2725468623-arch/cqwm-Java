package com.sky.handler;
import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Result<Void> duplicate() { return Result.error("用户名或分类名称已存在"); }
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> missingParameter() { return Result.error("缺少必要参数"); }
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> parameterType() { return Result.error("请求参数类型不正确"); }
    @ExceptionHandler(BaseException.class)
    public Result<Void> business(BaseException ex) { return Result.error(ex.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> validation(Exception ex) {
        org.springframework.validation.BindingResult result = ex instanceof MethodArgumentNotValidException
                ? ((MethodArgumentNotValidException) ex).getBindingResult() : ((BindException) ex).getBindingResult();
        return Result.error(result.getFieldErrors().isEmpty() ? "请求参数不正确"
                : result.getFieldErrors().get(0).getDefaultMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> unreadable() { return Result.error("请求内容格式不正确"); }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> unexpected(Exception ex) {
        log.error("请求处理失败", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
