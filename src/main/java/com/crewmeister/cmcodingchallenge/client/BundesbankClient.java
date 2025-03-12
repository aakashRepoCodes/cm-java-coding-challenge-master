package com.crewmeister.cmcodingchallenge.client;

import com.crewmeister.cmcodingchallenge.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateResponse;

import java.util.List;
import java.util.stream.Stream;

public interface BundesbankClient {
    List<CurrencyDTO> fetchCurrencies();
    List<ExchangeRateResponse> fetchExchangeRates(String date);
    Stream<String> fetchAllExchangeRatesCsv();
}
