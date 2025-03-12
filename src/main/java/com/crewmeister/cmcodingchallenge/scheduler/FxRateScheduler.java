package com.crewmeister.cmcodingchallenge.scheduler;

import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class FxRateScheduler {

    private final ExchangeRateService exchangeRateService ;
    private static final Logger logger = LoggerFactory.getLogger(FxRateScheduler.class);

    public FxRateScheduler(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Every night 1:am refresh of exchange rate.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void refreshCurrencies() {
        logger.info("Refreshing currencies from Bundesbank...");
        exchangeRateService.initFX();
    }
}

