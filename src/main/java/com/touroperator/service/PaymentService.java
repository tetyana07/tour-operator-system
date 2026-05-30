package com.touroperator.service;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Payment;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;

    public PaymentService(BookingRepository bookingRepo, PaymentRepository paymentRepo) {
        this.bookingRepo = bookingRepo;
        this.paymentRepo = paymentRepo;
    }

    public Payment pay(UUID bookingId) {
        return pay(bookingId, "ONLINE");
    }

    public Payment pay(UUID bookingId, String method) {
        Booking booking = bookingRepo.findById(bookingId)
              .orElseThrow(() -> new RuntimeException("Бронювання не знайдено: " + bookingId));

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus("SUCCESS");
        payment.setMethod(method != null ? method : "ONLINE");

        paymentRepo.save(payment);
        bookingRepo.markPaid(bookingId);
        return payment;
    }
}