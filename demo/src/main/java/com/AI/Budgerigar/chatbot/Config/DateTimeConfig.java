package com.AI.Budgerigar.chatbot.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class DateTimeConfig {

    @Bean
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
    }

}
