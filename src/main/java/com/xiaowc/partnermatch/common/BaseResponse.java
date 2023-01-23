package com.xiaowc.partnermatch.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类对象，返回给前端的对象
 * 目的：给对象补充一些信息，告诉前端这个请求在业务层面上是成功还是失败，200、404、500、502、503
 * 原来：
 *   返回 {
 *     "nama": "xiaowc"
 * }
 * 现在：
 *   成功 {
 *     "code": 0 //业务状态码
 *     "data": {
 *         "name": "xiaowc"
 *     }
 *     "message": "ok"
 *     "description": "message信息更详细的描述"
 * }
 *   失败 {
 *     "code": 50001 //业务状态码
 *     "data": null
 *     "message": "用户操作异常，xxx"
 *     "description": "message信息更详细的描述"
 * }
 *
 * @param <T> 使用泛型，代表这是一个通用返回类，提高整个对象的复用性
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 返回给前端的业务状态码
     */
    private int code;

    /**
     * 返回给前端的数据
     */
    private T data;

    /**
     * 状态码的消息，简写
     */
    private String message;

    /**
     * 状态码的描述，详细
     */
    private String description;

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(int code, T data, String message) {
        this(code, data, message, "");
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    /**
     * 从errorCode中取code,message,description
     * @param errorCode
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }
}
