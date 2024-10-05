package com.AI.Budgerigar.chatbot.Services;

import com.AI.Budgerigar.chatbot.DTO.UserIpInfoDTO;
import com.AI.Budgerigar.chatbot.Nosql.UserIpInfo;
import com.AI.Budgerigar.chatbot.Nosql.UserIpInfoRepository;
import com.AI.Budgerigar.chatbot.Entity.UserPw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class LoginIpService {

    // 权重配置，可以调整各字段的权重值
    private static final Map<String, Integer> FIELD_WEIGHTS = Map.ofEntries(Map.entry("AreaCode", 1),
            Map.entry("City", 2), Map.entry("CityCode", 1), // 与City字段关联，权重较小
            Map.entry("Continent", 3), Map.entry("Country", 3), Map.entry("CountryEnglish", 1),
            Map.entry("District", 1), Map.entry("Elevation", 1), Map.entry("Ip", 5), // IP变化权重大
            Map.entry("Isp", 2), Map.entry("Lat", 1), Map.entry("Lng", 1), Map.entry("Prov", 2),
            Map.entry("TimeZone", 3), Map.entry("WeatherStation", 1), Map.entry("ZipCode", 1));

    @Autowired
    private UserIpInfoRepository userIpInfoRepository;

    // 判断两个 IP 信息是否有变化，并返回变化字段数量
    public int countIpChanges(UserIpInfo oldIpInfo, UserIpInfoDTO.IpInfoDTO newIpInfo) {
        int totalChangeWeight = 0;

        // 按权重检查变化
        totalChangeWeight += !oldIpInfo.getAreaCode().equals(newIpInfo.getAreaCode()) ? FIELD_WEIGHTS.get("AreaCode")
                : 0;
        totalChangeWeight += !oldIpInfo.getCity().equals(newIpInfo.getCity()) ? FIELD_WEIGHTS.get("City") : 0;
        totalChangeWeight += !oldIpInfo.getCityCode().equals(newIpInfo.getCityCode()) ? FIELD_WEIGHTS.get("CityCode")
                : 0;
        totalChangeWeight += !oldIpInfo.getContinent().equals(newIpInfo.getContinent()) ? FIELD_WEIGHTS.get("Continent")
                : 0;
        totalChangeWeight += !oldIpInfo.getCountry().equals(newIpInfo.getCountry()) ? FIELD_WEIGHTS.get("Country") : 0;
        totalChangeWeight += !oldIpInfo.getCountryEnglish().equals(newIpInfo.getCountryEnglish())
                ? FIELD_WEIGHTS.get("CountryEnglish") : 0;
        totalChangeWeight += !oldIpInfo.getDistrict().equals(newIpInfo.getDistrict()) ? FIELD_WEIGHTS.get("District")
                : 0;
        totalChangeWeight += !oldIpInfo.getElevation().equals(newIpInfo.getElevation()) ? FIELD_WEIGHTS.get("Elevation")
                : 0;
        totalChangeWeight += !oldIpInfo.getIp().equals(newIpInfo.getIp()) ? FIELD_WEIGHTS.get("Ip") : 0;
        totalChangeWeight += !oldIpInfo.getIsp().equals(newIpInfo.getIsp()) ? FIELD_WEIGHTS.get("Isp") : 0;
        totalChangeWeight += !oldIpInfo.getLat().equals(newIpInfo.getLat()) ? FIELD_WEIGHTS.get("Lat") : 0;
        totalChangeWeight += !oldIpInfo.getLng().equals(newIpInfo.getLng()) ? FIELD_WEIGHTS.get("Lng") : 0;
        totalChangeWeight += !oldIpInfo.getProv().equals(newIpInfo.getProv()) ? FIELD_WEIGHTS.get("Prov") : 0;
        totalChangeWeight += !oldIpInfo.getTimeZone().equals(newIpInfo.getTimeZone()) ? FIELD_WEIGHTS.get("TimeZone")
                : 0;
        totalChangeWeight += !oldIpInfo.getWeatherStation().equals(newIpInfo.getWeatherStation())
                ? FIELD_WEIGHTS.get("WeatherStation") : 0;
        totalChangeWeight += !oldIpInfo.getZipCode().equals(newIpInfo.getZipCode()) ? FIELD_WEIGHTS.get("ZipCode") : 0;

        return totalChangeWeight;
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

        int totalChangeWeight = countIpChanges(oldIpInfo, newIpInfo);

        if (totalChangeWeight > 10) {
            // 变化权重大，认为是新的IP
            userIpInfoRepository.save(mapToUserIpInfo(user.getUuid(), newIpInfo));
            return LoginIpStatus.NEW;
        }
        else if (totalChangeWeight > 5) {
            // 有显著变化，认为是已变动
            userIpInfoRepository.save(mapToUserIpInfo(user.getUuid(), newIpInfo));
            return LoginIpStatus.CHANGED;
        }

        // 变化权重较小，认为没有变化
        return LoginIpStatus.NO_CHANGE;
    }

    public enum LoginIpStatus {

        NEW, CHANGED, NO_CHANGE

    }

}
