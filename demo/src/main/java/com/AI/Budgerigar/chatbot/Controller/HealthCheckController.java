package com.AI.Budgerigar.chatbot.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
public class HealthCheckController {

    @GetMapping("/health")
    public String healthCheck() {
        log.info("Service is running!!!");
        return "Service is running";
    }

}
