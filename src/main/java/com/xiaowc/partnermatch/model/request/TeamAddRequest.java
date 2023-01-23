package com.xiaowc.partnermatch.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * request属于dto的一种：
 * request层：用来存放用来接收前端请求的controller层的参数
 *   只对应controller方法中的形参(也就是前端传过来的数据)
 * 添加队伍请求体
 */
@Data
public class TeamAddRequest implements Serializable {

    /**
     * 对象自动生成的序列号
     */
    private static final long serialVersionUID = -5218637662871203316L;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private Date expireTime;

    /**
     * 创建队伍的人的用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 入队密码
     */
    private String password;

}
