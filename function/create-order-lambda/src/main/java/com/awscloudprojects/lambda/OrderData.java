package com.awscloudprojects.lambda;

public class OrderData {
    private String orderId;
    private String orderTime;
    public OrderData() {
    }
    public OrderData(String orderId, String orderTime) {
        this.orderId = orderId;
        this.orderTime = orderTime;
    }
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }
}
