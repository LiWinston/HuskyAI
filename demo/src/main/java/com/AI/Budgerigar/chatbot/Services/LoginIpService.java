package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.UserIpInfoDTO;
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
    public int countIpChanges(UserIpInfo oldIpInfo, UserIpInfoDTO.IpInfoDTO newIpInfo) {
        int changes = 0;
        changes += !oldIpInfo.getAreaCode().equals(newIpInfo.getAreaCode()) ? 1 : 0;
        changes += !oldIpInfo.getCity().equals(newIpInfo.getCity()) ? 1 : 0;
        changes += !oldIpInfo.getCityCode().equals(newIpInfo.getCityCode()) ? 1 : 0;
        changes += !oldIpInfo.getContinent().equals(newIpInfo.getContinent()) ? 1 : 0;
        changes += !oldIpInfo.getCountry().equals(newIpInfo.getCountry()) ? 1 : 0;
        changes += !oldIpInfo.getCountryEnglish().equals(newIpInfo.getCountryEnglish()) ? 1 : 0;
        changes += !oldIpInfo.getDistrict().equals(newIpInfo.getDistrict()) ? 1 : 0;
        changes += !oldIpInfo.getElevation().equals(newIpInfo.getElevation()) ? 1 : 0;
        changes += !oldIpInfo.getIp().equals(newIpInfo.getIp()) ? 1 : 0;
        changes += !oldIpInfo.getIsp().equals(newIpInfo.getIsp()) ? 1 : 0;
        changes += !oldIpInfo.getLat().equals(newIpInfo.getLat()) ? 1 : 0;
        changes += !oldIpInfo.getLng().equals(newIpInfo.getLng()) ? 1 : 0;
        changes += !oldIpInfo.getProv().equals(newIpInfo.getProv()) ? 1 : 0;
        changes += !oldIpInfo.getTimeZone().equals(newIpInfo.getTimeZone()) ? 1 : 0;
        changes += !oldIpInfo.getWeatherStation().equals(newIpInfo.getWeatherStation()) ? 1 : 0;
        changes += !oldIpInfo.getZipCode().equals(newIpInfo.getZipCode()) ? 1 : 0;
        return changes;
    }

    // 将 LoginDTO.UserIpInfoDTO 映射到 UserIpInfo
    private UserIpInfo mapToUserIpInfo(String userUuid, UserIpInfoDTO.IpInfoDTO newIpInfo) {
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

    public LoginIpStatus handleLoginIp(UserPw user, UserIpInfoDTO.IpInfoDTO newIpInfo) {
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
