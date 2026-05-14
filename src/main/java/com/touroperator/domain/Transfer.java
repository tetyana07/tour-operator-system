package com.touroperator.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Transfer {
    private UUID id;
    private String type;
    private BigDecimal price;

    public Transfer() {}

    public Transfer(String type, BigDecimal price) {
        this.type = type;
        this.price = price;
    }

    public Transfer(UUID id, String type, BigDecimal price) {
        this.id = id;
        this.type = type;
        this.price = price;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @Override
    public String toString() {
        return String.format("Transfer [id=%s, type='%s', price=%f]", id, type, price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transfer transfer = (Transfer) o;
        return id.equals(transfer.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}