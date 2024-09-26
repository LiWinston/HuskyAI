package com.AI.Budgerigar.chatbot.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserIpInfoDTO {

    private LoginDTO loginDTO;

    private IpInfoDTO ipInfoDTO;

    @Data
    @Builder
    public static class IpInfoDTO {

        @JsonProperty("area_code")
        private String areaCode;

        @JsonProperty("city")
        private String city;

        @JsonProperty("city_code")
        private String cityCode;

        @JsonProperty("continent")
        private String continent;

        @JsonProperty("country")
        private String country;

        @JsonProperty("country_english")
        private String countryEnglish;

        @JsonProperty("district")
        private String district;

        @JsonProperty("elevation")
        private String elevation;

        @JsonProperty("ip")
        private String ip;

        @JsonProperty("isp")
        private String isp;

        @JsonProperty("lat")
        private String lat;

        @JsonProperty("lng")
        private String lng;

        @JsonProperty("prov")
        private String prov;

        @JsonProperty("time_zone")
        private String timeZone;

        @JsonProperty("weather_station")
        private String weatherStation;

        @JsonProperty("zip_code")
        private String zipCode;

        public String toString() {
            return "UserIpInfoDTO{" + "areaCode='" + areaCode + '\'' + ", city='" + city + '\'' + ", cityCode='"
                    + cityCode + '\'' + ", continent='" + continent + '\'' + ", country='" + country + '\''
                    + ", countryEnglish='" + countryEnglish + '\'' + ", district='" + district + '\'' + ", elevation='"
                    + elevation + '\'' + ", ip='" + ip + '\'' + ", isp='" + isp + '\'' + ", lat='" + lat + '\''
                    + ", lng='" + lng + '\'' + ", prov='" + prov + '\'' + ", timeZone='" + timeZone + '\''
                    + ", weatherStation='" + weatherStation + '\'' + ", zipCode='" + zipCode + '\'' + '}';
        }

        public String toText() {
            return "areaCode: " + areaCode + "\n" + "city: " + city + "\n" + "cityCode: " + cityCode + "\n"
                    + "continent: " + continent + "\n" + "country: " + country + "\n" + "countryEnglish: "
                    + countryEnglish + "\n" + "district: " + district + "\n" + "elevation: " + elevation + "\n" + "ip: "
                    + ip + "\n" + "isp: " + isp + "\n" + "lat: " + lat + "\n" + "lng: " + lng + "\n" + "prov: " + prov
                    + "\n" + "timeZone: " + timeZone + "\n" + "weatherStation: " + weatherStation + "\n" + "zipCode: "
                    + zipCode + "\n";
        }

    }

}