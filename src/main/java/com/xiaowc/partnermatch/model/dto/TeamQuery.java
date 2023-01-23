package com.xiaowc.partnermatch.model.dto;

import com.xiaowc.partnermatch.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * dto层：业务封装类
 *
 * 队伍查询封装类，在这里和request层看起来是一样的，主要用来做查询
 *
 * 为什么需要请求参数包装类？
 *   1.请求参数名称/类型和实体类不一样。
 *   2.有一些参数用不到，如果要自动生成接口文档，会增加理解成本。
 *   3.多个字段映射到同一个对象。
 * 为什么需要包装类？
 *   1.可能有些字段需要隐藏，不能返回给前端，或者有些字段某些方法是不关心的。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * id列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词(同时对队伍名称和描述搜索)
     */
    private String searchText;

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
     * 创建队伍的人的用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

}
