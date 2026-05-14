package com.touroperator.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Insurance {
    private UUID id;
    private String name;
    private BigDecimal price;

    public Insurance() {}

    public Insurance(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public Insurance(UUID id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @Override
    public String toString() {
        return String.format("Insurance [id=%s, name='%s', price=%f]", id, name, price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Insurance insurance = (Insurance) o;
        return id.equals(insurance.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}