package com.touroperator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


public class Booking {
    private UUID id;
    private UUID clientId;
    private UUID tourId;
    private UUID promoCodeId;
    private int touristCount;
    private LocalDate bookingDate;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String status;
    private BigDecimal totalPrice;

    public Booking() {}

    public Booking(UUID id, UUID clientId, UUID tourId,
          UUID promoCodeId, int touristCount,
          LocalDate bookingDate, String status,
          BigDecimal totalPrice) {
        this.id = id;
        this.clientId = clientId;
        this.tourId = tourId;
        this.promoCodeId = promoCodeId;
        this.touristCount = touristCount;
        this.bookingDate = bookingDate;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public UUID getTourId() { return tourId; }
    public void setTourId(UUID tourId) { this.tourId = tourId; }

    public UUID getPromoCodeId() { return promoCodeId; }
    public void setPromoCodeId(UUID promoCodeId) { this.promoCodeId = promoCodeId; }

    public int getTouristCount() { return touristCount; }
    public void setTouristCount(int touristCount) { this.touristCount = touristCount; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    @Override
    public String toString() {
        return "Booking[id=" + id + ", status=" + status +
              ", tourists=" + touristCount + ", total=" + totalPrice + "]";
    }
}
