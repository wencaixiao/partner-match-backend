package com.xiaowc.partnermatch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * request属于dto的一种：
 * request层：用来存放用来接收前端请求的controller层的参数
 *   只对应controller方法中的形参(也就是前端传过来的数据)
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 对象自动生成的序列号
     */
    private static final long serialVersionUID = 7775568102716827770L;

    /**
     * 用户账户
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户校验密码
     */
    private String checkPassword;

    /**
     * 星球编号
     */
    private String planetCode;

}
