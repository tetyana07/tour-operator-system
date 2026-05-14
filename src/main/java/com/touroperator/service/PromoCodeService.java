package com.touroperator.service;

import com.touroperator.domain.PromoCode;
import com.touroperator.repository.PromoCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PromoCodeService {

    private final PromoCodeRepository promoRepo;

    @Autowired
    public PromoCodeService(PromoCodeRepository promoRepo) {
        this.promoRepo = promoRepo;
    }

    public List<PromoCode> findAll() {
        return promoRepo.findAll();
    }

    public Optional<PromoCode> findByCode(String code) {
        return promoRepo.findByCode(code);
    }

    public void save(PromoCode promo) {
        if (promo.getId() == null) promo.setId(UUID.randomUUID());
        promoRepo.save(promo);
    }

    public void delete(UUID id) {
        promoRepo.delete(id);
    }
}