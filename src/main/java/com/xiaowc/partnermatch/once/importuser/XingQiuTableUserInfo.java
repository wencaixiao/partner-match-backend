package com.xiaowc.partnermatch.once.importuser;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 星球表格用户信息
 * 见官网：https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
 */
@Data
public class XingQiuTableUserInfo {

    /**
     * id
     */
    @ExcelProperty("成员编号") // 和表格的字段名相匹配
    private String planetCode;

    /**
     * 用户昵称
     */
    @ExcelProperty("成员昵称") // 和表格的字段名相匹配
    private String username;

}