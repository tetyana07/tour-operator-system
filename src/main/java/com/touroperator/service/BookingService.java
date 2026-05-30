package com.touroperator.service;

import com.touroperator.domain.*;
import com.touroperator.dto.BookingRequest;

import com.touroperator.domain.BookingStatus;
import com.touroperator.exception.*;
import com.touroperator.repository.*;
import com.touroperator.specification.BookingByClientEmailSpec;
import com.touroperator.uow.UnitOfWork;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final Validator VALIDATOR =
          Validation.buildDefaultValidatorFactory().getValidator();

    private final BookingRepository    bookingRepo;
    private final TourRepository       tourRepo;
    private final ClientRepository     clientRepo;
    private final ExcursionRepository  excursionRepo;
    private final InsuranceRepository  insuranceRepo;
    private final TransferRepository   transferRepo;
    private final PromoCodeRepository  promoRepo;
    private final PaymentRepository    paymentRepo;
    private final PricingService       pricingService;
    private final NotificationService  notificationService;
    private final EmailService         emailService;
    private final UnitOfWork           unitOfWork;
    private final BookingAuditService  auditService;

    public BookingService(BookingRepository bookingRepo,
          TourRepository tourRepo,
          ClientRepository clientRepo,
          ExcursionRepository excursionRepo,
          InsuranceRepository insuranceRepo,
          TransferRepository transferRepo,
          PromoCodeRepository promoRepo,
          PaymentRepository paymentRepo,
          PricingService pricingService,
          NotificationService notificationService,
          EmailService emailService,
          UnitOfWork unitOfWork,
          BookingAuditService auditService) {
        this.bookingRepo         = bookingRepo;
        this.tourRepo            = tourRepo;
        this.clientRepo          = clientRepo;
        this.excursionRepo       = excursionRepo;
        this.insuranceRepo       = insuranceRepo;
        this.transferRepo        = transferRepo;
        this.promoRepo           = promoRepo;
        this.paymentRepo         = paymentRepo;
        this.pricingService      = pricingService;
        this.notificationService = notificationService;
        this.emailService        = emailService;
        this.unitOfWork          = unitOfWork;
        this.auditService        = auditService;
    }


    public PriceBreakdown previewPrice(BookingRequest req) {
        Tour tour = tourRepo.findById(req.getTourId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Тур", req.getTourId()));

        return pricingService.calculate(
              tour,
              req.getTouristCount(),
              req.getExcursionIds(),
              req.getInsuranceId(),
              req.getTransferId(),
              req.getPromoCode()
        );
    }


    @Transactional
    public Booking createBooking(BookingRequest req) {
           
        var violations = VALIDATOR.validate(req);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                  .map(ConstraintViolation::getMessage)
                  .findFirst().orElse("Помилка валідації запиту");
            throw new IllegalArgumentException(msg);
        }
        log.info("Створення бронювання: тур={}, туристи={}", req.getTourId(), req.getTouristCount());


        Tour tour = tourRepo.findById(req.getTourId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Тур", req.getTourId()));



        int available = tour.getAvailableSeats();
        if (req.getTouristCount() > available) {
            throw new QuotaExceededException(req.getTouristCount(), available);
        }
        if (req.getTouristCount() <= 0) {
            throw new IllegalArgumentException("Кількість туристів має бути більше 0");
        }


        clientRepo.findById(req.getClientId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Клієнт", req.getClientId()));


        if (bookingRepo.existsActiveByClientAndTour(req.getClientId(), req.getTourId())) {
            throw new InvalidBookingStateException(
                  "Клієнт вже має активне бронювання на цей тур. " +
                        "Скасуйте попереднє бронювання перш ніж створювати нове.");
        }


        UUID promoCodeId = null;
        if (req.getPromoCode() != null && !req.getPromoCode().isBlank()) {
            PromoCode pc = promoRepo.findByCode(req.getPromoCode().trim())
                  .orElse(null);
            if (pc != null) {
                if (!pc.isValid()) {
                    throw new PromoCodeExpiredException(
                          "Промокод '" + pc.getCode() + "' прострочений (дійсний до " + pc.getValidUntil() + ")");
                }
                promoCodeId = pc.getId();
            }
        }


        PriceBreakdown breakdown = pricingService.calculate(
              tour,
              req.getTouristCount(),
              req.getChildCount(),
              req.getExcursionIds(),
              req.getInsuranceId(),
              req.getTransferId(),
              req.getPromoCode(),
              req.getExtraDiscountPercent()
        );

        log.info("Розрахунок ціни: {}", breakdown);


        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setClientId(req.getClientId());
        booking.setTourId(req.getTourId());
        booking.setPromoCodeId(promoCodeId);
        booking.setTouristCount(req.getTouristCount());
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(BookingStatus.CREATED.name());
        booking.setTotalPrice(breakdown.getFinalPrice());


           
        bookingRepo.save(booking);
        tourRepo.incrementBookedSeats(tour.getId(), req.getTouristCount());


        if (req.getExcursionIds() != null && !req.getExcursionIds().isEmpty()) {
            excursionRepo.saveBookingExcursions(booking.getId(), req.getExcursionIds());
        }
        if (req.getInsuranceId() != null) {
            insuranceRepo.saveBookingInsurance(booking.getId(), req.getInsuranceId());
        }
        if (req.getTransferId() != null) {
            transferRepo.saveBookingTransfer(booking.getId(), req.getTransferId());
        }

        log.info("Бронювання створено: {} | Ціна: {} USD", booking.getId(), booking.getTotalPrice());
        try {
            com.touroperator.domain.Client client = clientRepo.findById(req.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingCreated(tour.getName(), clientName);
            auditService.log(booking.getId(), clientName, "CREATE", null,
                  BookingStatus.CREATED.name(),
                  "Тур: " + tour.getName() + " | Туристів: " + req.getTouristCount()
                        + " | Сума: " + booking.getTotalPrice());
        } catch (Exception ignored) {}
        return booking;
    }

    @Transactional
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.CREATED,
              "Можна підтвердити тільки бронювання зі статусом CREATED");
        bookingRepo.confirm(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        log.info("Бронювання підтверджено: {}", bookingId);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            com.touroperator.domain.Client client = clientRepo.findById(booking.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingConfirmed(tourName, clientName);
               
            String dates = tour != null && tour.getStartDate() != null
                  ? tour.getStartDate() + " – " + tour.getEndDate() : "";
            notificationService.notifyClientBookingConfirmed(tourName, dates.isBlank() ? "" : "Дати: " + dates);
               
            if (client != null && client.getEmail() != null) {
                emailService.sendBookingConfirmed(
                      client.getEmail(), clientName, tourName,
                      dates.isBlank() ? "" : dates);
            }
            auditService.log(bookingId, "SYSTEM", "CONFIRM",
                  BookingStatus.CREATED.name(), BookingStatus.CONFIRMED.name(),
                  "Тур: " + tourName + (dates.isBlank() ? "" : " | " + dates));
        } catch (Exception ignored) {}
        return booking;
    }


    public Payment payBooking(UUID bookingId, String method) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.CONFIRMED,
              "Можна оплатити тільки підтверджене бронювання (CONFIRMED)");

           
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
        bookingRepo.markPaid(bookingId);
        log.info("Оплата успішна: booking={}, amount={}, method={}", bookingId, booking.getTotalPrice(), method);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            notificationService.notifyBookingPaid(tourName, String.format("%,.0f", booking.getTotalPrice()));
               
            notificationService.notifyClientPaymentReceived(tourName,
                  "₴" + String.format("%,.0f", booking.getTotalPrice()));
               
            com.touroperator.domain.Client clientForEmail =
                  clientRepo.findById(booking.getClientId()).orElse(null);
            if (clientForEmail != null && clientForEmail.getEmail() != null) {
                emailService.sendPaymentReceived(
                      clientForEmail.getEmail(),
                      clientForEmail.getName(),
                      tourName,
                      "₴" + String.format("%,.0f", booking.getTotalPrice()));
            }
            auditService.log(bookingId,
                  clientForEmail != null ? clientForEmail.getName() : "SYSTEM",
                  "PAY", BookingStatus.CONFIRMED.name(), BookingStatus.PAID.name(),
                  "Сума: ₴" + String.format("%,.0f", booking.getTotalPrice()) + " | Метод: " + method);
        } catch (Exception ignored) {}
        return payment;
    }

    @Transactional
    public void completeBooking(UUID bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.PAID,
              "Можна завершити тільки оплачене бронювання (PAID)");
        bookingRepo.markCompleted(bookingId);
        log.info("Бронювання завершено: {}", bookingId);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            if (tour != null) notificationService.notifyBookingCompleted(tour.getName());
            auditService.log(bookingId, "SYSTEM", "COMPLETE",
                  BookingStatus.PAID.name(), BookingStatus.COMPLETED.name(),
                  tour != null ? "Тур: " + tour.getName() : null);
        } catch (Exception e) {
            log.warn("Помилка сповіщення при завершенні: {}", e.getMessage());
        }
    }


    @Transactional
    public void cancelBooking(UUID bookingId, String reason) {
        Booking booking = getBookingOrThrow(bookingId);

        String status = booking.getStatus();
        if (BookingStatus.CANCELLED.name().equals(status) ||
              BookingStatus.COMPLETED.name().equals(status)) {
            throw new InvalidBookingStateException(
                  "Не можна скасувати бронювання зі статусом " + status);
        }

        bookingRepo.cancel(bookingId, reason);
        tourRepo.decrementBookedSeats(booking.getTourId(), booking.getTouristCount());

           
        if (BookingStatus.PAID.name().equals(status)) {
            Payment refund = new Payment();
            refund.setId(UUID.randomUUID());
            refund.setBookingId(bookingId);
            refund.setAmount(booking.getTotalPrice().negate());    
            refund.setPaymentDate(java.time.LocalDate.now());
            refund.setStatus("REFUND");
            paymentRepo.save(refund);
            log.info("Рефанд створено: booking={}, сума={}", bookingId, booking.getTotalPrice());
        }

        log.info("Бронювання скасовано: {}, місця повернуто: {}", bookingId, booking.getTouristCount());
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            com.touroperator.domain.Client client = clientRepo.findById(booking.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingCancelled(tourName, clientName);
            String refundNote = BookingStatus.PAID.name().equals(status)
                  ? "Кошти буде повернуто протягом 3–5 робочих днів."
                  : "Якщо є питання — зверніться до підтримки.";
            notificationService.notifyClientBookingCancelled(tourName, refundNote);
            if (client != null && client.getEmail() != null) {
                emailService.sendBookingCancelled(
                      client.getEmail(), clientName, tourName, refundNote);
            }
            auditService.log(bookingId, clientName, "CANCEL",
                  status, BookingStatus.CANCELLED.name(),
                  "Причина: " + reason + (BookingStatus.PAID.name().equals(status) ? " | Рефанд: ₴" + booking.getTotalPrice() : ""));
        } catch (Exception e) {
            log.warn("Помилка надсилання сповіщення про скасування: {}", e.getMessage());
        }
    }



    public Booking getBookingById(UUID id) {
        return getBookingOrThrow(id);
    }

    public List<Booking> getBookingsByClient(UUID clientId) {
        return bookingRepo.findByClientId(clientId);
    }

    public List<Booking> getBookingsByTour(UUID tourId) {
        return bookingRepo.findByTourId(tourId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepo.findAll();
    }



    private Booking getBookingOrThrow(UUID id) {
        return bookingRepo.findById(id)
              .orElseThrow(() ->
                    new EntityNotFoundException("Бронювання", id));
    }

    private void assertStatus(Booking booking, BookingStatus expected, String message) {
        if (!expected.name().equals(booking.getStatus())) {
            throw new InvalidBookingStateException(
                  message + ". Поточний статус: " + booking.getStatus());
        }
    }


    public List<Booking> findAll() {
        return getAllBookings();
    }


    public Booking createBooking(UUID clientId, UUID tourId,
          int adults, int children, String promoCode) {
        com.touroperator.dto.BookingRequest req = new com.touroperator.dto.BookingRequest();
        req.setClientId(clientId);
        req.setTourId(tourId);
        req.setTouristCount(adults + children);
        req.setPromoCode(promoCode);
        return createBooking(req);
    }

    /**
     * Пошук бронювань за email клієнта через Specification (без SQL injection).
     */
    public List<Booking> findByClientEmail(String email) {
        return bookingRepo.findBySpec(new BookingByClientEmailSpec(email));
    }
}