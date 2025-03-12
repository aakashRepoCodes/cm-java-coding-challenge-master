package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.client.BundesbankClientImpl;
import com.crewmeister.cmcodingchallenge.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.model.CurrencyEntity;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class CurrencyService {

    private final BundesbankClientImpl bundesbankClientImpl;
    private final CurrencyRepository currencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    public CurrencyService(BundesbankClientImpl bundesbankClientImpl, CurrencyRepository currencyRepository) {
        this.bundesbankClientImpl = bundesbankClientImpl;
        this.currencyRepository = currencyRepository;
    }

    /**
     * Loads currencies at startup.
     */
    @PostConstruct
    public void loadCurrenciesAtStartup() {
        try {
            logger.info("Loading currencies into DB...");
            List<CurrencyDTO> currencies = bundesbankClientImpl.fetchCurrencies();
            List<CurrencyEntity> currencyEntities = currencies.stream()
                    .map(currency -> new CurrencyEntity(currency.getCode(), currency.getName()))
                    .collect(Collectors.toList());
            currencyRepository.saveAll(currencyEntities);
        } catch (Exception e) {
            logger.error("Failed to load currencies at startup: {}", e.getMessage());
        }
    }

    /**
     * Retrieves all available currencies from the local database.
     *  * If the database is empty, it fetches currencies from the Bundesbank API,
     *  * saves them locally, and then returns them.
     *
     */
    public List<CurrencyDTO> getAllCurrencies() {
        if (currencyRepository.count() > 0) {
            return currencyRepository.findAll().stream()
                    .map(entity -> new CurrencyDTO(entity.getCode(), entity.getName()))
                    .collect(Collectors.toList());
        }
        List<CurrencyDTO> fetchedCurrencies = bundesbankClientImpl.fetchCurrencies();
        if (!fetchedCurrencies.isEmpty()) {
            List<CurrencyEntity> currencyEntities = fetchedCurrencies.stream()
                    .filter(currencyDTO -> !currencyRepository.existsByCode(currencyDTO.getCode())) // Avoid duplicate insertion
                    .map(currencyDTO -> new CurrencyEntity(currencyDTO.getCode(), currencyDTO.getName()))
                    .collect(Collectors.toList());
            currencyRepository.saveAll(currencyEntities);
        }
        return fetchedCurrencies;
    }
}


