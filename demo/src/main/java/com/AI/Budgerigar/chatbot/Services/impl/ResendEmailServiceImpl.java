package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Services.EmailService;
import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service("resendEmailService")
public class ResendEmailServiceImpl implements EmailService {

    @Autowired
    private Resend resendClient;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(content)
                .build();

            CreateEmailResponse data = resendClient.emails().send(params);
            log.info("Email sent successfully to: {}, id: {}", to, data.getId());
        }
        catch (ResendException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

}