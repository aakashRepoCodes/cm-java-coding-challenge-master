package com.crewmeister.cmcodingchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangedCurrencyAmountResponse {

    private String currency;
    private String date;
    private double originalAmount;
    private double exchangeRate;
    private double convertedAmount;
}

