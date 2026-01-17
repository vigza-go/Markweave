package com.vigza.markweave.common;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常
      * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e){
        log.warn("业务异常:{}",e.getMessage());
        return Result.error(e.getCode(),e.getMessage());
    }

    /**
     * 捕获系统未知运行时异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e){
        log.error("系统运行异常:{}",e.getMessage());
        return Result.error(500,"系统繁忙，请稍后再试");
    }

    /**
     * 捕获参数校验异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e){
        log.error("其它异常:{}",e.getMessage());
        return Result.error(500,"服务器异常");
    }
}
