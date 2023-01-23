package com.xiaowc.partnermatch.model.enums;

/**
 * 队伍的状态枚举
 *  0 - 公开
 *  1 - 私有
 *  2 - 加密
 */
public enum TeamStatusEnum {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    /**
     * 枚举值
     */
    private int value;

    /**
     * 枚举值对应的说明
     */
    private String text;

    /**
     * 根据枚举的值获取整个枚举
     * @param value 枚举对应的值
     * @return
     */
    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values(); // 拿到全部枚举值
        for (TeamStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
