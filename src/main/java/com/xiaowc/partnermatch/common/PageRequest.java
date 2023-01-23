package com.xiaowc.partnermatch.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 自动生成的序列化id
     */
    private static final long serialVersionUID = 4105547678829892404L;

    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当签是第几页
     */
    protected int pageNum = 1;
}
