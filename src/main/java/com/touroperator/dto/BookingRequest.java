package com.touroperator.dto;

import java.util.List;
import java.util.UUID;


public class BookingRequest {

    private UUID clientId;
    private UUID tourId;
    private int touristCount;
    private List<UUID> excursionIds;
    private UUID insuranceId;
    private UUID transferId;
    private String promoCode;

    private int childCount;

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