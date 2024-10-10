package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.DTO.UserIpInfoDTO;
import com.AI.Budgerigar.chatbot.Services.LoginIpService;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class NotifyUserIssueAspect {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${sudo.alert.email}")
    private String SudoAlertEmail;

    /**
     * Define a cut point and execute aspect logic. When "handleLoginIp", return
     * "LoginIpStatus.NEW".
     * @param usr user information
     * @param loginIpInfo new IP information
     */
    @AfterReturning(
            pointcut = "execution(* com.AI.Budgerigar.chatbot.Services.LoginIpService.handleLoginIp(..)) && args(usr, loginIpInfo)",
            returning = "result", argNames = "usr,loginIpInfo,result")
    private void reportToAdmin(UserPw usr, UserIpInfoDTO loginIpInfo, LoginIpService.LoginIpStatus result) {
        switch (result) {
            case NEW, CHANGED:
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(SudoAlertEmail);
                message.setSubject("User" + usr.getUsername() + " logged in from a new IP");
                message.setText("User: " + usr.getUsername() + "\n" + "UUID: " + usr.getUuid() + "\n" + "Role: "
                        + usr.getRole() + "\n" + "Logged in from a new IP:\n" + loginIpInfo.getIpInfoDTO().toText());
                log.info("Reported to admin. User: {} logged in from a new IP.", usr.getUsername());
                mailSender.send(message);
                break;
            case NO_CHANGE:
                log.info("User: {} logged in from the same IP.", usr.getUsername());
                break;
            default:
                break;
        }

    }

}
