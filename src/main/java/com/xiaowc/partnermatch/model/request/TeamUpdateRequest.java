package com.xiaowc.partnermatch.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * request属于dto的一种：
 * request层：用来存放用来接收前端请求的controller层的参数
 *   只对应controller方法中的形参(也就是前端传过来的数据)
 * 修改队伍信息请求体
 */
@Data
public class TeamUpdateRequest implements Serializable {

    /**
     * 对象自动生成的序列号
     */
    private static final long serialVersionUID = 7030040027120043209L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 入队密码
     */
    private String password;

}
