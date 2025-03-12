package com.crewmeister.cmcodingchallenge.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "exchange_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String currencyCode;
    private String targetCurrency;
    private String date;
    private double exchangeRate;

    public ExchangeRateEntity(String currencyCode, String targetCurrency, String date, double exchangeRate) {
        this.currencyCode = currencyCode;
        this.targetCurrency = targetCurrency;
        this.date = date;
        this.exchangeRate = exchangeRate;
    }
}

