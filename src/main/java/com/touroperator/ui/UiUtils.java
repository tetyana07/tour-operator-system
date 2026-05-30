package com.touroperator.ui;


public final class UiUtils {

    private UiUtils() {}


    public static String countryFlag(String country) {
        if (country == null) return "🌍";
        return switch (country.toLowerCase().trim()) {
            case "туреччина", "turkey"          -> "🇹🇷";
            case "єгипет", "egypt"              -> "🇪🇬";
            case "іспанія", "spain"             -> "🇪🇸";
            case "греція", "greece"             -> "🇬🇷";
            case "italy", "італія"              -> "🇮🇹";
            case "france", "франція"            -> "🇫🇷";
            case "thailand", "таїланд"          -> "🇹🇭";
            case "croatia", "хорватія"          -> "🇭🇷";
            case "maldives", "мальдіви"         -> "🇲🇻";
            case "czech republic", "чехія"      -> "🇨🇿";
            case "austria", "австрія"           -> "🇦🇹";
            case "germany", "німеччина"         -> "🇩🇪";
            case "ukraine", "україна"           -> "🇺🇦";
            case "portugal", "португалія"       -> "🇵🇹";
            case "morocco", "марокко"           -> "🇲🇦";
            case "japan", "японія"              -> "🇯🇵";
            case "bali", "балі", "indonesia", "індонезія" -> "🇮🇩";
            case "dubai", "дубай", "uae", "оае" -> "🇦🇪";
            default -> "🌍";
        };
    }

    /** Локалізує статус бронювання з англійського в українську. */
    public static String localizeBookingStatus(String status) {
        if (status == null) return "Невідомо";
        return switch (status) {
            case "CREATED"   -> "Нове";
            case "CONFIRMED" -> "Підтверджено";
            case "PAID"      -> "Сплачено";
            case "COMPLETED" -> "Завершено";
            case "CANCELLED" -> "Скасовано";
            default -> status;
        };
    }

    /** Повертає CSS-клас для pill-статусу бронювання. */
    public static String bookingStatusPillClass(String status) {
        if (status == null) return "pill-pending";
        return switch (status) {
            case "CREATED"   -> "pill-pending";
            case "CONFIRMED" -> "pill-confirmed";
            case "PAID"      -> "pill-paid";
            case "COMPLETED" -> "pill-paid";
            case "CANCELLED" -> "pill-cancelled";
            default          -> "pill-pending";
        };
    }
}