package com.xiaowc.partnermatch.once.importuser;

import com.alibaba.excel.EasyExcel;

import java.util.List;

/**
 * 导入 Excel
 * 见官网：https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
 */
public class ImportExcel {

    /**
     * 读取数据
     */
    public static void main(String[] args) {
        // 写法1：JDK8+
        // since: 3.0.0-beta1
        String fileName = "E:\\HUST\\Happiness\\项目\\伙伴匹配系统\\partner-match-backend\\src\\main\\resources\\testExcel.xlsx";
//        readByListener(fileName);
        synchronousRead(fileName);
    }

    /**
     * 第一种读取方式：通过监听器读取
     *   先创建监听器、在读取文件时绑定监听器，单独抽离处理逻辑，代码清晰易于维护，一条一条处理，适用于数据量大的场景
     * @param fileName
     */
    public static void readByListener(String fileName) {
        // 读取的文件名，数据的对象类型，监听器
        EasyExcel.read(fileName, XingQiuTableUserInfo.class, new TableListener()).sheet().doRead();
    }

    /**
     * 第二种读取方式：同步读，不用再去定义一个监听器了，比较简单
     *   无需创建监听器，一次性获取完整数据，方便简单，但是数据量大时会有等待时常，也可能内存溢出
     * 对于我们这种场景，第二种方式肯定要更简单
     * @param fileName
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<XingQiuTableUserInfo> totalDataList =
                EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        for (XingQiuTableUserInfo xingQiuTableUserInfo : totalDataList) {
            System.out.println(xingQiuTableUserInfo);
        }
    }

}
