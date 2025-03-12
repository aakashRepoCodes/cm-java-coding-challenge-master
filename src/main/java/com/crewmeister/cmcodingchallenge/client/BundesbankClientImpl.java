package com.crewmeister.cmcodingchallenge.client;

import com.crewmeister.cmcodingchallenge.config.BundesbankConfig;
import com.crewmeister.cmcodingchallenge.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.exception.BundesbankException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.internal.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class BundesbankClientImpl implements BundesbankClient {

    private final WebClient webClient;
    private final BundesbankConfig config;
    private static final Logger logger = LoggerFactory.getLogger(BundesbankClientImpl.class);

    private final BundesbankClientResponseParser bundesbankClientResponseParser;

    public BundesbankClientImpl(WebClient.Builder webClientBuilder, BundesbankConfig config, BundesbankClientResponseParser bundesbankClientResponseParser) {
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        this.config = config;
        this.bundesbankClientResponseParser = bundesbankClientResponseParser;
    }

    /**
     * Fetch currencies from Bundesbank API.
     */
    @Override
    public List<CurrencyDTO> fetchCurrencies() {
        return fetchData(config.getCurrenciesEndpoint(), bundesbankClientResponseParser::parseCurrencies);
    }

    /**
     * Fetch exchange rates for the given currency.
     */
    @Override
    public List<ExchangeRateResponse> fetchExchangeRates(String date) {
        String url = config.getExchangeRatesEndpoint(date);
        return fetchData(url, bundesbankClientResponseParser::parseExchangeRates);
    }


    @Override
    public Stream<String> fetchAllExchangeRatesCsv() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(config.getDataSetEndPoint()))
                    .GET()
                    .header("Accept", "text/csv")
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                throw new BundesbankException("Unexpected response from Bundesbank: " + response.statusCode(), HttpStatus.BAD_REQUEST);
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundesbankException("Thread interrupted while fetching exchange rates", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (URISyntaxException e) {
            throw new BundesbankException("Invalid Bundesbank API URL: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            throw new BundesbankException("Network error while fetching exchange rates: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Generic method to fetch data from Bundesbank API.
     */
    private <T> List<T> fetchData(String url, Function<String, List<T>> parser) {
        try {
            Map<String, String> headers = config.determineHeaders(url);

            String jsonResponse = webClient.get()
                    .uri(url)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return parser.apply(jsonResponse);

        } catch (WebClientResponseException.NotFound e) {
            logger.error("Bundesbank API returned 404: {}", url);
            throw new BundesbankException("Bundesbank API resource not found", HttpStatus.NOT_FOUND);

        } catch (WebClientResponseException.BadRequest e) {
            logger.error("Bundesbank API returned 400: {}", url);
            throw new BundesbankException("Invalid request to Bundesbank API", HttpStatus.BAD_REQUEST);

        } catch (WebClientResponseException e) {
            logger.error("Bundesbank API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BundesbankException("Bundesbank API error: " + e.getStatusCode(), e.getStatusCode());

        } catch (Exception e) {
            logger.error("Unexpected error fetching data: {}", e.getMessage());
            throw new BundesbankException("Unexpected error occurred while fetching data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}


