package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.LoginDTO;
import com.AI.Budgerigar.chatbot.Nosql.UserIpInfo;
import com.AI.Budgerigar.chatbot.Nosql.UserIpInfoRepository;
import com.AI.Budgerigar.chatbot.model.UserPw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginIpService {

    @Autowired
    private UserIpInfoRepository userIpInfoRepository;

    // 判断两个 IP 信息是否有变化，并返回变化字段数量
    public int countIpChanges(UserIpInfo oldIpInfo, LoginDTO.UserIpInfoDTO newIpInfo) {
        int changes = 0;
        if (!oldIpInfo.getAreaCode().equals(newIpInfo.getAreaCode()))
            changes++;
        if (!oldIpInfo.getCity().equals(newIpInfo.getCity()))
            changes++;
        if (!oldIpInfo.getCityCode().equals(newIpInfo.getCityCode()))
            changes++;
        if (!oldIpInfo.getContinent().equals(newIpInfo.getContinent()))
            changes++;
        if (!oldIpInfo.getCountry().equals(newIpInfo.getCountry()))
            changes++;
        if (!oldIpInfo.getCountryEnglish().equals(newIpInfo.getCountryEnglish()))
            changes++;
        if (!oldIpInfo.getDistrict().equals(newIpInfo.getDistrict()))
            changes++;
        if (!oldIpInfo.getElevation().equals(newIpInfo.getElevation()))
            changes++;
        if (!oldIpInfo.getIp().equals(newIpInfo.getIp()))
            changes++;
        if (!oldIpInfo.getIsp().equals(newIpInfo.getIsp()))
            changes++;
        if (!oldIpInfo.getLat().equals(newIpInfo.getLat()))
            changes++;
        if (!oldIpInfo.getLng().equals(newIpInfo.getLng()))
            changes++;
        if (!oldIpInfo.getProv().equals(newIpInfo.getProv()))
            changes++;
        if (!oldIpInfo.getTimeZone().equals(newIpInfo.getTimeZone()))
            changes++;
        if (!oldIpInfo.getWeatherStation().equals(newIpInfo.getWeatherStation()))
            changes++;
        if (!oldIpInfo.getZipCode().equals(newIpInfo.getZipCode()))
            changes++;
        return changes;
    }

    // 将 LoginDTO.UserIpInfoDTO 映射到 UserIpInfo
    private UserIpInfo mapToUserIpInfo(String userUuid, LoginDTO.UserIpInfoDTO newIpInfo) {
        return UserIpInfo.builder()
            .userUuid(userUuid)
            .areaCode(newIpInfo.getAreaCode())
            .city(newIpInfo.getCity())
            .cityCode(newIpInfo.getCityCode())
            .continent(newIpInfo.getContinent())
            .country(newIpInfo.getCountry())
            .countryEnglish(newIpInfo.getCountryEnglish())
            .district(newIpInfo.getDistrict())
            .elevation(newIpInfo.getElevation())
            .ip(newIpInfo.getIp())
            .isp(newIpInfo.getIsp())
            .lat(newIpInfo.getLat())
            .lng(newIpInfo.getLng())
            .prov(newIpInfo.getProv())
            .timeZone(newIpInfo.getTimeZone())
            .weatherStation(newIpInfo.getWeatherStation())
            .zipCode(newIpInfo.getZipCode())
            .build();
    }

    public LoginIpStatus handleLoginIp(UserPw user, LoginDTO.UserIpInfoDTO newIpInfo) {
        log.info("Handling login IP for user: {} {}", user.getUuid(), newIpInfo);
        UserIpInfo oldIpInfo = userIpInfoRepository.findByUserUuid(user.getUuid()).orElse(null);

        if (oldIpInfo == null) {
            // 第一次记录该用户的 IP 信息，保存到数据库
            userIpInfoRepository.save(mapToUserIpInfo(user.getUuid(), newIpInfo));
            return LoginIpStatus.NEW;
        }

        int changes = countIpChanges(oldIpInfo, newIpInfo);

        if (changes > 6) {
            // 变动超过6项，认为是新的IP
            userIpInfoRepository.save(mapToUserIpInfo(user.getUuid(), newIpInfo));
            return LoginIpStatus.NEW;
        }
        else if (changes > 2) {
            // 变动超过2项，认为发生了变化
            userIpInfoRepository.save(mapToUserIpInfo(user.getUuid(), newIpInfo));
            return LoginIpStatus.CHANGED;
        }

        return LoginIpStatus.NO_CHANGE;
    }

    public enum LoginIpStatus {

        NEW, CHANGED, NO_CHANGE

    }

}
