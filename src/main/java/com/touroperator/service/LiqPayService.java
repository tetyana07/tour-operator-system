package com.touroperator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;


@Service
public class LiqPayService {

    private static final Logger log = LoggerFactory.getLogger(LiqPayService.class);

    private static final String CHECKOUT_URL = "https://www.liqpay.ua/api/3/checkout";
    private static final String API_VERSION  = "3";
    private static final String ACTION_PAY   = "pay";

    private final String  publicKey;
    private final String  privateKey;
    private final boolean sandbox;

    public LiqPayService(
          @Value("${liqpay.public.key:sandbox_demo_public_key}")  String publicKey,
          @Value("${liqpay.private.key:sandbox_demo_private_key}") String privateKey,
          @Value("${liqpay.sandbox:true}")                         boolean sandbox) {
        this.publicKey  = publicKey;
        this.privateKey = privateKey;
        this.sandbox    = sandbox;
    }

       

    /**
     * Генерує URL для оплати бронювання.
     *
     * @param amount      сума до списання (UAH за замовчуванням)
     * @param currency    валюта: {@code "UAH"}, {@code "USD"}, {@code "EUR"}
     * @param description текст, що відображається на сторінці LiqPay
     * @param orderId     унікальний ID замовлення (UUID бронювання)
     * @return  готове посилання для відкриття у браузері
     * @throws LiqPayException якщо не вдалося сформувати підпис
     */
    public String generatePaymentUrl(BigDecimal amount, String currency,
          String description, String orderId) {
        validateAmount(amount);
        String amountStr = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        Map<String, String> params = buildParams(amountStr, currency, description, orderId);
        try {
            String data      = encodeData(params);
            String signature = sign(data);
            String url = CHECKOUT_URL
                  + "?data="      + URLEncoder.encode(data,      StandardCharsets.UTF_8)
                  + "&signature=" + URLEncoder.encode(signature,  StandardCharsets.UTF_8);
            log.info("LiqPay URL сформовано: orderId={}, amount={} {}, sandbox={}",
                  orderId, amountStr, currency, sandbox);
            return url;
        } catch (Exception e) {
            throw new LiqPayException("Не вдалося сформувати LiqPay URL для orderId=" + orderId, e);
        }
    }

    /**
     * Перевіряє підпис callback-відповіді від LiqPay.
     *
     * <p>LiqPay надсилає POST з параметрами {@code data} і {@code signature}.
     * Якщо підписи збігаються — платіж легітимний.
     *
     * @param data      Base64-рядок із відповіді LiqPay
     * @param signature підпис із відповіді LiqPay
     * @return {@code true}, якщо підпис валідний
     */
    public boolean verifyCallback(String data, String signature) {
        try {
            String expected = sign(data);
            boolean valid = expected.equals(signature);
            if (!valid) {
                log.warn("LiqPay callback: невалідний підпис. data={}", data);
            }
            return valid;
        } catch (Exception e) {
            log.error("LiqPay verifyCallback помилка", e);
            return false;
        }
    }

    /**
     * Розшифровує data-параметр callback і повертає статус платежу.
     *
     * @param data Base64-рядок із відповіді LiqPay
     * @return рядок статусу: {@code "success"}, {@code "failure"}, {@code "sandbox"} тощо
     */
    public String extractStatus(String data) {
        try {
            String json = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
               
            return extractJsonField(json, "status");
        } catch (Exception e) {
            log.error("Не вдалося розшифрувати LiqPay data", e);
            return "unknown";
        }
    }

    /**
     * Витягує orderId із callback-даних.
     *
     * @param data Base64-рядок із відповіді LiqPay
     * @return значення поля {@code order_id}
     */
    public String extractOrderId(String data) {
        try {
            String json = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
            return extractJsonField(json, "order_id");
        } catch (Exception e) {
            log.error("Не вдалося витягти order_id із LiqPay data", e);
            return "";
        }
    }

       

    /**
     * Кодує параметри платежу у Base64(JSON).
     * Порядок ключів гарантований через {@link LinkedHashMap}.
     */
    public String encodeData(Map<String, String> params) throws Exception {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(escape(e.getKey())).append("\":")
                  .append("\"").append(escape(e.getValue())).append("\"");
            first = false;
        }
        json.append("}");
        return Base64.getEncoder().encodeToString(
              json.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Формує підпис: {@code Base64( SHA-1( privateKey + data + privateKey ) )}.
     */
    public String sign(String data) throws Exception {
        MessageDigest md   = MessageDigest.getInstance("SHA-1");
        byte[]        hash = md.digest(
              (privateKey + data + privateKey).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

       

    private Map<String, String> buildParams(String amount, String currency,
          String description, String orderId) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("public_key",  publicKey);
        p.put("version",     API_VERSION);
        p.put("action",      ACTION_PAY);
        p.put("amount",      amount);
        p.put("currency",    currency);
        p.put("description", description);
        p.put("order_id",    orderId);
        if (sandbox) p.put("sandbox", "1");
        return p;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LiqPayException("Сума платежу має бути більше нуля, отримано: " + amount);
        }
    }

    /** Витягує значення поля з JSON без зовнішніх бібліотек. */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        int end   = json.indexOf('"', start + 1);
        if (start < 0 || end < 0) return "";
        return json.substring(start + 1, end);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

       

    public boolean isSandbox()    { return sandbox; }
    public String  getPublicKey() { return publicKey; }

       

    /**
     * Виключення, що сигналізує про помилку при роботі з LiqPay API.
     */
    public static class LiqPayException extends RuntimeException {
        public LiqPayException(String message) {
            super(message);
        }
        public LiqPayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}