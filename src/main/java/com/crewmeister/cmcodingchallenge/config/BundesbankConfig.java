package com.crewmeister.cmcodingchallenge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.crewmeister.cmcodingchallenge.util.BundesbankConstants.LANGUAGE;

@Component
public class BundesbankConfig {

    @Value("${bundesbank.api.base-url}")
    private String baseUrl;

    @Value("${bundesbank.api.currencies}")
    private String currenciesEndpoint;

    @Value("${bundesbank.api.exchange-rates}")
    private String exchangeRatesEndpoint;

    @Value("${bundesbank.api.headers.data}")
    private String dataHeader;

    @Value("${bundesbank.api.headers.structure}")
    private String structureHeader;

    @Value("${bundesbank.api.dataset}")
    private String bundesbankApiUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCurrenciesEndpoint() {
        return baseUrl + currenciesEndpoint;
    }

    public String getDataSetEndPoint() {
        return bundesbankApiUrl;
    }


    public String getExchangeRatesEndpoint(String date) {
        String seriesKey = "D..EUR.BB.AC.000";
        return "/rest/data/BBEX3/" + seriesKey + "?startPeriod=" + date + "&endPeriod=" + date + "&detail=dataonly";
    }

    public Map<String, String> determineHeaders(String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT_LANGUAGE, LANGUAGE);
        if (url.contains("/data/BBEX3/")) {
            headers.put(HttpHeaders.ACCEPT, dataHeader);
        } else {
            headers.put(HttpHeaders.ACCEPT, structureHeader);
        }
        return headers;
    }
}

