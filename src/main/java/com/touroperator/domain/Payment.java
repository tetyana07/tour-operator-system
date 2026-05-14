package com.touroperator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Payment {
    private UUID id;
    private UUID bookingId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String status;
    private String method;

    public Payment() {}

    public Payment(UUID bookingId, BigDecimal amount, LocalDate paymentDate, String status) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    public Payment(UUID id, UUID bookingId, BigDecimal amount, LocalDate paymentDate, String status) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    @Override
    public String toString() {
        return String.format("Payment[id=%s, amount=%s, status=%s]", id, amount, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment p = (Payment) o;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}
