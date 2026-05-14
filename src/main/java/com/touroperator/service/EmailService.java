package com.touroperator.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);


    private final String smtpHost;
    private final int    smtpPort;
    private final boolean smtpAuth;
    private final boolean smtpStartTls;
    private final String fromAddress;
    private final String username;
    private final String password;
    private final boolean dryRun;


    private final ExecutorService executor =
          Executors.newSingleThreadExecutor(r -> {
              Thread t = new Thread(r, "email-sender");
              t.setDaemon(true);
              return t;
          });

    public EmailService() {
        Properties p = loadProps();
        this.smtpHost     = p.getProperty("mail.smtp.host",     "smtp.gmail.com");
        this.smtpPort     = Integer.parseInt(p.getProperty("mail.smtp.port", "587"));
        this.smtpAuth     = Boolean.parseBoolean(p.getProperty("mail.smtp.auth",     "true"));
        this.smtpStartTls = Boolean.parseBoolean(p.getProperty("mail.smtp.starttls", "true"));
        this.fromAddress  = p.getProperty("mail.from",     "noreply@ayvo.ua");
        this.username     = p.getProperty("mail.username", "");
        this.password     = p.getProperty("mail.password", "");
        this.dryRun       = Boolean.parseBoolean(p.getProperty("mail.dry.run", "true"));

        log.info("EmailService ініціалізовано: host={}:{}, dryRun={}", smtpHost, smtpPort, dryRun);
    }


    public void sendBookingConfirmed(String toEmail, String clientName,
          String tourName, String dates) {
        String subject = "✅ Ваше бронювання підтверджено — " + tourName;
        String body = buildHtml(
              "Бронювання підтверджено!",
              clientName,
              "Ваше бронювання туру <strong>" + tourName + "</strong> успішно підтверджено.",
              dates.isBlank() ? "" : "<p>📅 Дати туру: <strong>" + dates + "</strong></p>",
              "Дякуємо, що обрали AYVO Travel!"
        );
        sendAsync(toEmail, subject, body);
    }

    public void sendPaymentReceived(String toEmail, String clientName,
          String tourName, String amount) {
        String subject = "💳 Оплату отримано — " + tourName;
        String body = buildHtml(
              "Оплату зараховано!",
              clientName,
              "Ми отримали вашу оплату за тур <strong>" + tourName + "</strong>.",
              "<p>💰 Сума: <strong>" + amount + "</strong></p>",
              "Гарної подорожі! ✈"
        );
        sendAsync(toEmail, subject, body);
    }

    public void sendBookingCancelled(String toEmail, String clientName,
          String tourName, String refundNote) {
        String subject = "❌ Бронювання скасовано — " + tourName;
        String body = buildHtml(
              "Бронювання скасовано",
              clientName,
              "На жаль, ваше бронювання туру <strong>" + tourName + "</strong> було скасовано.",
              "<p>ℹ️ " + refundNote + "</p>",
              "Якщо у вас є питання, зв'яжіться з нами: support@ayvo.ua"
        );
        sendAsync(toEmail, subject, body);
    }


    public void sendTourReminder(String toEmail, String clientName,
          String tourName, long daysLeft) {
        String subject = daysLeft == 0
              ? "✈ Сьогодні ваш виліт — " + tourName
              : "🗓 До туру залишилося " + daysLeft + " дн. — " + tourName;
        String message = daysLeft == 0
              ? "Сьогодні починається ваш тур <strong>" + tourName + "</strong>! Щасливої подорожі!"
              : "До початку туру <strong>" + tourName + "</strong> залишилося <strong>"
                    + daysLeft + " дн.</strong> Не забудьте підготуватися!";
        String body = buildHtml("Нагадування про тур", clientName, message, "", "До зустрічі! 🌍");
        sendAsync(toEmail, subject, body);
    }

    public void sendRaw(String toEmail, String subject, String htmlBody) {
        sendAsync(toEmail, subject, htmlBody);
    }


    private void sendAsync(String toEmail, String subject, String htmlBody) {
        executor.submit(() -> {
            try {
                doSend(toEmail, subject, htmlBody);
            } catch (Exception e) {
                log.error("Помилка надсилання листа на {}: {}", toEmail, e.getMessage(), e);
            }
        });
    }

    private void doSend(String toEmail, String subject, String htmlBody)
          throws MessagingException, java.io.UnsupportedEncodingException {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("Пропущено надсилання: некоректна адреса '{}'", toEmail);
            return;
        }

        if (dryRun) {
            log.info("[DRY-RUN] Лист до: {} | Тема: {} | Body:\n{}", toEmail, subject,
                  htmlBody.replaceAll("<[^>]+>", "").trim());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host",           smtpHost);
        props.put("mail.smtp.port",           smtpPort);
        props.put("mail.smtp.auth",           smtpAuth);
        props.put("mail.smtp.starttls.enable", smtpStartTls);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout",           "10000");
        props.put("mail.smtp.writetimeout",      "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromAddress, "AYVO Travel", StandardCharsets.UTF_8.name()));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(msg);
        log.info("Лист надіслано: to={}, subject={}", toEmail, subject);
    }

    private String buildHtml(String heading, String clientName,
          String mainText, String extraHtml,
          String footer) {
        return """
               <!DOCTYPE html>
               <html lang="uk">
               <head>
                 <meta charset="UTF-8">
                 <meta name="viewport" content="width=device-width, initial-scale=1.0">
                 <title>AYVO Travel</title>
               </head>
               <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;">
                 <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:30px 0;">
                   <tr><td align="center">
                     <table width="600" cellpadding="0" cellspacing="0"
                            style="background:#ffffff;border-radius:12px;overflow:hidden;
                                   box-shadow:0 2px 8px rgba(0,0,0,.08);">
                       <!-- Header -->
                       <tr>
                         <td style="background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 50%%,#0f3460 100%%);
                                    padding:32px 40px;text-align:center;">
                           <h1 style="margin:0;color:#e94560;font-size:28px;letter-spacing:2px;">AYVO Travel</h1>
                           <p  style="margin:6px 0 0;color:#a0aec0;font-size:13px;">Система управління турами</p>
                         </td>
                       </tr>
                       <!-- Body -->
                       <tr>
                         <td style="padding:36px 40px;">
                           <h2 style="margin:0 0 8px;color:#1a202c;font-size:22px;">%s</h2>
                           <p  style="margin:0 0 20px;color:#4a5568;font-size:15px;">
                             Привіт, <strong>%s</strong>!
                           </p>
                           <p style="color:#2d3748;font-size:15px;line-height:1.6;">%s</p>
                           %s
                           <hr style="border:none;border-top:1px solid #e2e8f0;margin:28px 0;">
                           <p style="color:#718096;font-size:13px;margin:0;">%s</p>
                         </td>
                       </tr>
                       <!-- Footer -->
                       <tr>
                         <td style="background:#f7fafc;padding:20px 40px;text-align:center;
                                    border-top:1px solid #e2e8f0;">
                           <p style="margin:0;color:#a0aec0;font-size:12px;">
                             © 2025 AYVO Travel · Цей лист надіслано автоматично, не відповідайте на нього.<br>
                             <a href="mailto:support@ayvo.ua"
                                style="color:#e94560;text-decoration:none;">support@ayvo.ua</a>
                           </p>
                         </td>
                       </tr>
                     </table>
                   </td></tr>
                 </table>
               </body>
               </html>
               """.formatted(heading, clientName, mainText, extraHtml, footer);
    }


    private Properties loadProps() {
        Properties p = new Properties();
        try (InputStream is = getClass().getClassLoader()
              .getResourceAsStream("application.properties")) {
            if (is != null) p.load(is);
        } catch (IOException e) {
            log.warn("Не вдалося завантажити application.properties: {}", e.getMessage());
        }
        return p;
    }
}