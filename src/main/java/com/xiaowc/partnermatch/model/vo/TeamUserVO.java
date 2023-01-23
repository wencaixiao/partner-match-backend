package com.xiaowc.partnermatch.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VO层：返回给前端数据的封装类
 *
 * 返回给前端的队伍和用户信息封装类(脱敏)
 */
@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = -6427311578785078667L;

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人的用户信息
     */
    private UserVO createUser;

    /**
     * 已加入该队伍的人数
     */
    private Integer hasJoinNum;

    /**
     * 用户是否已加入目前搜索出来的队伍，可以根据这个字段来交给前端展示，仅加入队伍和创建队伍的人能看到队伍操作按钮
     */
    private boolean hasJoin = false;

    /**
     * 队伍头像
     */
    private String avatarUrl;
}
