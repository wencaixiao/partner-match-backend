package com.xiaowc.partnermatch.exception;

import com.xiaowc.partnermatch.common.ErrorCode;

/**
 * 定义业务异常处理类
 * 其实就是给原本的运行时异常类扩充了两个字段，并且增加了构造函数，使其支持传递ErrorCode
 *   1.相对于java的异常类，支持更多的字段
 *   2.自定义构造函数，更灵活/快捷的设置字段
 *
 */
public class BusinessException extends RuntimeException{

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态的描述信息，详细
     */
    private final String description;

    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) { // errorCode传入的是ErrorCode枚举类中的某个枚举值
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
