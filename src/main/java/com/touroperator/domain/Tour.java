package com.touroperator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Сутність туру.
 * Містить квоту місць та кількість заброньованих.
 */
public class Tour {
    private UUID id;
    private String name;
    private String country;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal basePrice;
    private int quota;
    private int bookedSeats;
    private UUID hotelId;
    private TourStatus status;
    private String imagePath;
    // Що включено в тур
    private String mealType;       // null = не включено, "Сніданок", "Напівпансіон", "Повний пансіон", "Все включено"
    private boolean hasInsurance;
    private boolean hasTransfer;
    private boolean hasGuide;
    private boolean hasFlight;
    private String departure;   // Місто/аеропорт відльоту

    public Tour() {}

    public Tour(String name, String country, String city,
          LocalDate startDate, LocalDate endDate,
          BigDecimal basePrice, int quota, UUID hotelId) {
        this.name = name;
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.basePrice = basePrice;
        this.quota = quota;
        this.bookedSeats = 0;
        this.hotelId = hotelId;
    }

    public Tour(UUID id, String name, String country, String city,
          LocalDate startDate, LocalDate endDate,
          BigDecimal basePrice, int quota, int bookedSeats, UUID hotelId) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.basePrice = basePrice;
        this.quota = quota;
        this.bookedSeats = bookedSeats;
        this.hotelId = hotelId;
    }

    /** Повертає кількість вільних місць */
    public int getAvailableSeats() {
        return quota - bookedSeats;
    }

    /** Відсоток заповненості (0.0 - 1.0) */
    public double getFillRate() {
        if (quota == 0) return 0;
        return (double) bookedSeats / quota;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public int getQuota() { return quota; }
    public void setQuota(int quota) { this.quota = quota; }

    public int getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public TourStatus getStatus() { return status; }
    public void setStatus(TourStatus status) { this.status = status; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public boolean isHasInsurance() { return hasInsurance; }
    public void setHasInsurance(boolean hasInsurance) { this.hasInsurance = hasInsurance; }

    public boolean isHasTransfer() { return hasTransfer; }
    public void setHasTransfer(boolean hasTransfer) { this.hasTransfer = hasTransfer; }

    public boolean isHasGuide() { return hasGuide; }
    public void setHasGuide(boolean hasGuide) { this.hasGuide = hasGuide; }

    public boolean isHasFlight() { return hasFlight; }
    public void setHasFlight(boolean hasFlight) { this.hasFlight = hasFlight; }

    public String getDeparture() { return departure; }
    public void setDeparture(String departure) { this.departure = departure; }

    @Override
    public String toString() {
        return String.format("Tour[id=%s, name='%s', %s/%s, quota=%d, booked=%d]",
              id, name, country, city, quota, bookedSeats);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tour tour = (Tour) o;
        return id != null && id.equals(tour.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}