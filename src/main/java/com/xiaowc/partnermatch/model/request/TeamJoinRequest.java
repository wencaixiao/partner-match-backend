package com.xiaowc.partnermatch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * request属于dto的一种：
 * request层：用来存放用来接收前端请求的controller层的参数
 *   只对应controller方法中的形参(也就是前端传过来的数据)
 * 加入队伍请求体
 */
@Data
public class TeamJoinRequest implements Serializable {

    /**
     * 对象自动生成的序列号
     */
    private static final long serialVersionUID = 7030040027120043209L;

    /**
     * 用户加入的teamId
     */
    private Long teamId;
    /**
     * 入队密码
     */
    private String password;

}
