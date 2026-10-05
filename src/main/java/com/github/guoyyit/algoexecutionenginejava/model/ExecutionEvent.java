package com.github.guoyyit.algoexecutionenginejava.model;

public class ExecutionEvent {
    private long sequence;
    private long time;
    private String parentOrderId;
    private String childOrderId;
    private EventType type;
    private long quantity;
    private double price;
    private String detail;

    public ExecutionEvent() {
    }

    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    public String getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(String parentOrderId) { this.parentOrderId = parentOrderId; }
    public String getChildOrderId() { return childOrderId; }
    public void setChildOrderId(String childOrderId) { this.childOrderId = childOrderId; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
