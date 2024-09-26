package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.Nosql.UserIpInfo;
import com.AI.Budgerigar.chatbot.Nosql.UserIpInfoRepository;
import com.AI.Budgerigar.chatbot.model.UserPw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginIpService {

    @Autowired
    private UserIpInfoRepository userIpInfoRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${sudo.alert.email}")
    private String SudoAlertEmail;

    // 判断两个 IP 信息是否有变化，并返回变化字段数量
    public int countIpChanges(UserIpInfo oldIpInfo, UserIpInfo newIpInfo) {
        int changes = 0;
        if (!oldIpInfo.getContinent().equals(newIpInfo.getContinent())) changes++;
        if (!oldIpInfo.getCountry().equals(newIpInfo.getCountry())) changes++;
        if (!oldIpInfo.getOwner().equals(newIpInfo.getOwner())) changes++;
        if (!oldIpInfo.getIsp().equals(newIpInfo.getIsp())) changes++;
        if (!oldIpInfo.getZipcode().equals(newIpInfo.getZipcode())) changes++;
        if (!oldIpInfo.getTimezone().equals(newIpInfo.getTimezone())) changes++;
        if (!oldIpInfo.getAccuracy().equals(newIpInfo.getAccuracy())) changes++;
        if (!oldIpInfo.getAreacode().equals(newIpInfo.getAreacode())) changes++;
        if (!oldIpInfo.getAdcode().equals(newIpInfo.getAdcode())) changes++;
        if (!oldIpInfo.getAsnumber().equals(newIpInfo.getAsnumber())) changes++;
        if (!oldIpInfo.getLat().equals(newIpInfo.getLat())) changes++;
        if (!oldIpInfo.getLng().equals(newIpInfo.getLng())) changes++;
        if (!oldIpInfo.getRadius().equals(newIpInfo.getRadius())) changes++;
        if (!oldIpInfo.getProv().equals(newIpInfo.getProv())) changes++;
        if (!oldIpInfo.getCity().equals(newIpInfo.getCity())) changes++;
        if (!oldIpInfo.getDistrict().equals(newIpInfo.getDistrict())) changes++;
        return changes;
    }

    public LoginIpStatus handleLoginIp(String userUuid, UserIpInfo newIpInfo) {
        log.info("Handling login IP for user: {} {}", userUuid, newIpInfo);
        UserIpInfo oldIpInfo = userIpInfoRepository.findByUserUuid(userUuid).orElse(null);

        if (oldIpInfo == null) {
            // 第一次记录该用户的 IP 信息，保存到数据库
            userIpInfoRepository.save(newIpInfo);
            return LoginIpStatus.NEW;
        }

        int changes = countIpChanges(oldIpInfo, newIpInfo);

        if (changes > 6) {
            // 变动超过6项，认为是新的IP
            userIpInfoRepository.save(newIpInfo);
            return LoginIpStatus.NEW;
        } else if (changes > 2) {
            // 变动超过2项，认为发生了变化
            userIpInfoRepository.save(newIpInfo);
            return LoginIpStatus.CHANGED;
        }

        return LoginIpStatus.NO_CHANGE;
    }

    public void reportToAdmin(UserPw usr, UserIpInfo newIpInfo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("HuskyAI");
        message.setTo(SudoAlertEmail);
        message.setSubject("User {} logged in from a new IP".formatted(usr.getUsername()));
        message.setText("User " + usr
                + "| uuid: " + usr.getUuid()
                + "| role: " + usr.getRole()
                + "logged in from a new IP: \n\r" + newIpInfo);
        log.info("Reported to admin. User: {} logged in from a new IP.", usr.getUsername());
        mailSender.send(message);
    }

    public enum LoginIpStatus {
        NEW, CHANGED, NO_CHANGE
    }
}
