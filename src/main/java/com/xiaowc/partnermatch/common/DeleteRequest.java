package com.xiaowc.partnermatch.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求参数
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 4824285830884026676L;

    /**
     * id
     */
    private long id;
}
