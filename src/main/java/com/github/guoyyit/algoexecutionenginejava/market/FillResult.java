package com.github.guoyyit.algoexecutionenginejava.market;

public class FillResult {
    private long filledQuantity;
    private double price;

    public FillResult() {
    }

    public FillResult(long filledQuantity, double price) {
        this.filledQuantity = filledQuantity;
        this.price = price;
    }

    public long getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(long filledQuantity) { this.filledQuantity = filledQuantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
