package com.devteria.notification.controller;


import com.devteria.event.dto.NotificationEvent;
import com.devteria.notification.dto.request.Recipient;
import com.devteria.notification.dto.request.SendEmailRequest;
import com.devteria.notification.exception.AppException;
import com.devteria.notification.exception.ErrorCode;
import com.devteria.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    EmailService emailService;
    SpringTemplateEngine templateEngine;
    JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    @NonFinal
    String FROM;

    @KafkaListener(topics = "notification-delivery")
    public void listenNotificationDelivery(NotificationEvent request){
        log.info("Message received: {}", request);
        try {
            Date date = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("name", request.getName());
            context.setVariable("username", request.getUsername());
            context.setVariable("password", request.getPassword());
            context.setVariable("proton", "Proton " + year);
            context.setVariable("headerTab", request.getHeaderTab());
            String html = templateEngine.process("welcome-email", context);

            helper.setTo(request.getEmail());
            helper.setText(html, true);
            helper.setSubject("Tài khoản đối soát");
            helper.setFrom(FROM);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }
}
