package com.touroperator.dto;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

/**
 * DTO для створення бронювання.
 * Всі поля валідуються через Bean Validation (@Valid у сервісі).
 */
public class BookingRequest {

    @NotNull(message = "ID клієнта обов'язковий")
    private UUID clientId;

    @NotNull(message = "ID туру обов'язковий")
    private UUID tourId;

    @Min(value = 1, message = "Кількість туристів має бути не менше 1")
    @Max(value = 50, message = "Максимум 50 туристів на одне бронювання")
    private int touristCount;

    private List<UUID> excursionIds;

    private UUID insuranceId;
    private UUID transferId;

    @Size(max = 50, message = "Промокод не може перевищувати 50 символів")
    private String promoCode;

    @Min(value = 0, message = "Кількість дітей не може бути від'ємною")
    private int childCount;

    @Min(value = 0, message = "Додатковий discount не може бути від'ємним")
    @Max(value = 100, message = "Discount не може перевищувати 100%")
    private int extraDiscountPercent;

    public BookingRequest() {}

    public BookingRequest(UUID clientId, UUID tourId, int touristCount,
          List<UUID> excursionIds, UUID insuranceId,
          UUID transferId, String promoCode) {
        this.clientId = clientId;
        this.tourId = tourId;
        this.touristCount = touristCount;
        this.excursionIds = excursionIds;
        this.insuranceId = insuranceId;
        this.transferId = transferId;
        this.promoCode = promoCode;
    }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public UUID getTourId() { return tourId; }
    public void setTourId(UUID tourId) { this.tourId = tourId; }

    public int getTouristCount() { return touristCount; }
    public void setTouristCount(int touristCount) { this.touristCount = touristCount; }

    public List<UUID> getExcursionIds() { return excursionIds; }
    public void setExcursionIds(List<UUID> excursionIds) { this.excursionIds = excursionIds; }

    public UUID getInsuranceId() { return insuranceId; }
    public void setInsuranceId(UUID insuranceId) { this.insuranceId = insuranceId; }

    public UUID getTransferId() { return transferId; }
    public void setTransferId(UUID transferId) { this.transferId = transferId; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }

    public int getExtraDiscountPercent() { return extraDiscountPercent; }
    public void setExtraDiscountPercent(int extraDiscountPercent) { this.extraDiscountPercent = extraDiscountPercent; }
}
