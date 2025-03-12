package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.client.BundesbankClientImpl;
import com.crewmeister.cmcodingchallenge.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.model.CurrencyEntity;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrencyServiceTest {

    @Mock
    private BundesbankClientImpl bundesbankClient;

    @Mock
    private CurrencyRepository currencyRepository;

    private CurrencyService currencyService;
    private List<CurrencyDTO> mockCurrencyDTOs;
    private List<CurrencyEntity> mockCurrencyEntities;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService(bundesbankClient, currencyRepository);

        mockCurrencyDTOs = Arrays.asList(
                new CurrencyDTO("USD", "US Dollar"),
                new CurrencyDTO("EUR", "Euro"),
                new CurrencyDTO("GBP", "British Pound")
        );

        mockCurrencyEntities = Arrays.asList(
                new CurrencyEntity("USD", "US Dollar"),
                new CurrencyEntity("EUR", "Euro"),
                new CurrencyEntity("GBP", "British Pound")
        );
    }

    @Test
    void loadCurrenciesAtStartup_Success() {
        // Given
        when(bundesbankClient.fetchCurrencies()).thenReturn(mockCurrencyDTOs);

        // When
        currencyService.loadCurrenciesAtStartup();

        // Then
        verify(bundesbankClient, times(1)).fetchCurrencies();
        verify(currencyRepository, times(1)).saveAll(anyList());
    }

    @Test
    void loadCurrenciesAtStartup_HandlesException() {
        // Given
        when(bundesbankClient.fetchCurrencies()).thenThrow(new RuntimeException("API Error"));

        // When
        currencyService.loadCurrenciesAtStartup();

        // Then
        verify(bundesbankClient, times(1)).fetchCurrencies();
        verify(currencyRepository, never()).saveAll(anyList());
    }

    @Test
    void getAllCurrencies_WhenRepositoryHasData() {
        // Given
        when(currencyRepository.count()).thenReturn(3L);
        when(currencyRepository.findAll()).thenReturn(mockCurrencyEntities);

        // When
        List<CurrencyDTO> result = currencyService.getAllCurrencies();

        // Then
        verify(currencyRepository, times(1)).count();
        verify(currencyRepository, times(1)).findAll();
        verify(bundesbankClient, never()).fetchCurrencies();

        assertEquals(3, result.size());
        assertEquals("USD", result.get(0).getCode());
        assertEquals("US Dollar", result.get(0).getName());
    }

    @Test
    void getAllCurrencies_WhenRepositoryIsEmpty_FetchesFromClient() {
        // Given
        when(currencyRepository.count()).thenReturn(0L);
        when(bundesbankClient.fetchCurrencies()).thenReturn(mockCurrencyDTOs);
        when(currencyRepository.existsByCode(anyString())).thenReturn(false);

        // When
        List<CurrencyDTO> result = currencyService.getAllCurrencies();

        // Then
        verify(currencyRepository, times(1)).count();
        verify(bundesbankClient, times(1)).fetchCurrencies();
        verify(currencyRepository, times(1)).saveAll(anyList());

        assertEquals(3, result.size());
        assertEquals("USD", result.get(0).getCode());
        assertEquals("US Dollar", result.get(0).getName());
    }

}