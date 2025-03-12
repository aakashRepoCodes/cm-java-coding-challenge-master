package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.client.BundesbankClientImpl;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangedCurrencyAmountResponse;
import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);
    private final BundesbankClientImpl bundesbankClientImpl;
    final ExchangeRateRepository exchangeRateRepository;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public ExchangeRateService(BundesbankClientImpl bundesbankClientImpl, ExchangeRateRepository exchangeRateRepository) {
        this.bundesbankClientImpl = bundesbankClientImpl;
        this.exchangeRateRepository = exchangeRateRepository;
    }


    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initFX() {
        if (isUpdating.compareAndSet(false, true)) {
            try {
                fetchAndStoreAllExchangeRates();
            } finally {
                isUpdating.set(false);
            }
        }
    }

    public ExchangedCurrencyAmountResponse exchangeCurrencyWithEuro(String currency, String date, double amount) {
        // Euro to euro conversion should be dummy and no real REST call
        if ("EUR".equalsIgnoreCase(currency)) {
            return new ExchangedCurrencyAmountResponse(
                    currency.toUpperCase(),
                    date,
                    amount,
                    1.0,
                    amount
            );
        }
        ensureNotUpdating();
        ExchangeRateEntity exchangeRateEntity = exchangeRateRepository
                .findByCurrencyCodeAndDate(currency.toUpperCase(), date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exchange rate not available for " + currency + " on " + date));

        double rate = exchangeRateEntity.getExchangeRate();
        double convertedAmount = amount / rate;
        return new ExchangedCurrencyAmountResponse(
                currency.toUpperCase(),
                date,
                amount,
                rate,
                BigDecimal.valueOf(convertedAmount).setScale(2, RoundingMode.HALF_UP).doubleValue()
        );
    }

    protected ExchangeRateEntity parseCsvLine(String line) {
        try {
            String[] fields = line.split(";");
            if (fields.length < 9) return null;

            String currency = fields[2].trim();    // Target currency ( AUD, INR)
            String baseCurrency = fields[3].trim(); // Base currency (always EUR)
            LocalDate date = LocalDate.parse(fields[7].trim()); // Parse date (yyyy-MM-dd)

            String rateStr = fields[8].trim();
            if (".".equals(rateStr)) {
                return null; // Missing exchange rate, skip this row
            }

            double exchangeRate = Double.parseDouble(rateStr);
            return new ExchangeRateEntity(currency, baseCurrency, date.toString(), exchangeRate);

        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("Invalid CSV data: {}", line, e);
        } catch (Exception ex) {
            logger.error("Unexpected error while parsing CSV: {}", line, ex);
        }
        return null;
    }

    public void fetchAndStoreAllExchangeRates() {
        List<ExchangeRateEntity> newRates = bundesbankClientImpl.fetchAllExchangeRatesCsv()
                .skip(1)
                .map(this::parseCsvLine)
                .filter(Objects::nonNull)
                .filter(rate -> !exchangeRateRepository.existsByCurrencyCodeAndDate(rate.getCurrencyCode(), rate.getDate())) // Avoid duplicates
                .collect(Collectors.toList());

        if (!newRates.isEmpty()) {
            exchangeRateRepository.saveAll(newRates);
        }
        logger.info("Fetched {} exchange rates", newRates.size());
    }

    public Page<ExchangeRateEntity> fetchAndStoreAllExchangeRatesCsv(int page, int size) {
        ensureNotUpdating();
        Pageable pageable = PageRequest.of(page, size);
        return exchangeRateRepository.findAll(pageable);
    }


    public List<ExchangeRateResponse> getFxExchangeRatesOnDate(String date) {
        ensureNotUpdating();
        List<ExchangeRateEntity> storedRates = exchangeRateRepository.findByDate(date);
        if (!storedRates.isEmpty()) {
            logger.info("Returning {} stored exchange rates for date {}", storedRates.size(), date);
            return storedRates.stream()
                    .map(rate -> new ExchangeRateResponse(rate.getCurrencyCode(), rate.getExchangeRate(), rate.getDate()))
                    .collect(Collectors.toList());
        }
        List<ExchangeRateResponse> fetchedRates = bundesbankClientImpl.fetchExchangeRates(date);
        if (fetchedRates.isEmpty()) {
            logger.warn("No exchange rates available from external API for date {}", date);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No exchange rates available for " + date);
        }

        List<ExchangeRateEntity> entities = fetchedRates.stream()
                .map(rate -> new ExchangeRateEntity(rate.getCurrencyCode(), "EUR", rate.getDate(), rate.getExchangeRate()))
                .collect(Collectors.toList());
        exchangeRateRepository.saveAll(entities);

        logger.info("Stored {} fetched exchange rates for date {}", entities.size(), date);

        return fetchedRates;
    }

    /**
     *  Checks if system is updating and returns 503 if so.
     */
    public void ensureNotUpdating() {
        if (isUpdating.get()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is updating. Try again later.");
        }
    }

}

