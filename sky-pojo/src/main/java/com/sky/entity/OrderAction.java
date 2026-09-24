package com.sky.entity;

/**
 * 订单动作枚举
 * 定义订单生命周期中所有可能执行的操作
 */
public enum OrderAction {

    /**
     * 支付：待付款 -> 待接单
     */
    PAY("PAY", "支付", false),

    /**
     * 取消订单：待付款/待接单 -> 已取消
     */
    CANCEL("CANCEL", "取消订单", true),

    /**
     * 接单：待接单 -> 已接单
     */
    CONFIRM("CONFIRM", "接单", false),

    /**
     * 拒单：待接单 -> 已取消
     */
    REJECT("REJECT", "拒单", true),

    /**
     * 派送：已接单 -> 派送中
     */
    DELIVER("DELIVER", "派送", false),

    /**
     * 完成订单：派送中 -> 已完成
     */
    COMPLETE("COMPLETE", "完成订单", false),

    /**
     * 超时处理：待付款 -> 已取消 / 派送中 -> 已完成
     */
    SET_TIMEOUT("SET_TIMEOUT", "超时处理", false);

    /**
     * 动作编码（传给前端）
     */
    private final String code;

    /**
     * 中文标签（前端展示）
     */
    private final String label;

    /**
     * 执行时是否需要填写原因
     */
    private final boolean needReason;

    OrderAction(String code, String label, boolean needReason) {
        this.code = code;
        this.label = label;
        this.needReason = needReason;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isNeedReason() {
        return needReason;
    }
}
