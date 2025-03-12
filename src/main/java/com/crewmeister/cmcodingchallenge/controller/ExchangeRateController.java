package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.dto.ApiResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangedCurrencyAmountResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Fetch FX exchange rates for given date.
     */
    @GetMapping("/fx-exchange")
    public ResponseEntity<?> getExchangeRates(@RequestParam String date) {
        List<ExchangeRateResponse> rates = exchangeRateService.getFxExchangeRatesOnDate(date);
        return ResponseEntity.ok(new ApiResponse<>(rates));
    }

    @GetMapping("/currency-exchange-euro")
    public ResponseEntity<?> convertToEUR(
            @RequestParam String currency,
            @RequestParam String date,
            @RequestParam double amount
    ) {
        ExchangedCurrencyAmountResponse response = exchangeRateService.exchangeCurrencyWithEuro(currency, date, amount);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    /**
     * Get all available EUR-FX exchange rates as a paginated collection.
     */
    @GetMapping("/fx-exchange-dataset")
    public ResponseEntity<ApiResponse<Page<ExchangeRateEntity>>> paginatedExchangeRates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {

        Page<ExchangeRateEntity> paginatedRates = exchangeRateService.fetchAndStoreAllExchangeRatesCsv(page, size);

        if (page >= paginatedRates.getTotalPages() && paginatedRates.getTotalPages() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page number exceeds available pages : " + (paginatedRates.getTotalPages() - 1));
        }
        return ResponseEntity.ok(new ApiResponse<>(paginatedRates));
    }

}


