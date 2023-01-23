package com.xiaowc.partnermatch.common;


/**
 * 错误码：枚举类
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
 */
public enum ErrorCode {

    SUCCESS(0, "ok", ""),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无权限", ""),
    FORBIDDEN(40301, "禁止操作", ""),
    SYSTEM_ERROR(50000, "系统内部异常", "");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误码的消息，简写
     */
    private final String message;

    /**
     * 错误码的描述，详细
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
