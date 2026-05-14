package com.touroperator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Excursion {
    private UUID id;
    private String name;
    private BigDecimal price;
    private LocalDate excursionDate;

    public Excursion() {}

    public Excursion(String name, BigDecimal price, LocalDate excursionDate) {
        this.name = name;
        this.price = price;
        this.excursionDate = excursionDate;
    }

    public Excursion(UUID id, String name, BigDecimal price, LocalDate excursionDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.excursionDate = excursionDate;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDate getExcursionDate() { return excursionDate; }
    public void setExcursionDate(LocalDate excursionDate) { this.excursionDate = excursionDate; }

    @Override
    public String toString() {
        return String.format("Excursion [id=%s, name='%s', price=%f, excursionDate='%s']", id, name, price, excursionDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Excursion excursion = (Excursion) o;
        return id.equals(excursion.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}