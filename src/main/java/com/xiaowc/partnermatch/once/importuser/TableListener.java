package com.xiaowc.partnermatch.once.importuser;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 新建一个最简单的读的监听器，实现对应的接口
 * 见官网：https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
 */
@Slf4j
public class TableListener implements ReadListener<XingQiuTableUserInfo> {

    /**
     * 每读一条数据都会调用这个方法
     * @param data 当前读到的这条数据
     * @param context 上下文，当前读取的进度之类的
     */
    @Override
    public void invoke(XingQiuTableUserInfo data, AnalysisContext context) {
        System.out.println(data);
    }

    /**
     * 所有数据解析完成了 都会来调用
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("已解析完成");
    }
}