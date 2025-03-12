package com.crewmeister.cmcodingchallenge.client;

import com.crewmeister.cmcodingchallenge.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.util.BundesbankConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crewmeister.cmcodingchallenge.util.BundesbankConstants.*;

@Component
public class BundesbankClientResponseParser {

    private final Logger logger = LoggerFactory.getLogger(BundesbankClientResponseParser.class);

    /**
     * Parses JSON into a list of Currency objects.
     */
    List<CurrencyDTO> parseCurrencies(String jsonResponse) {
        List<CurrencyDTO> currencies = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray codeLists = root.getJSONObject(BundesbankConstants.DATA).getJSONArray(CODE_LISTS);
            if (!codeLists.isEmpty()) {
                JSONArray codes = codeLists.getJSONObject(0).getJSONArray(CODES);
                for (int i = 0; i < codes.length(); i++) {
                    JSONObject codeObj = codes.getJSONObject(i);
                    String currencyCode = codeObj.getString(ID);
                    String currencyName = codeObj.getJSONObject(NAMES).getString(LANGUAGE);
                    if (currencyCode.matches("^[A-Z]{3}$")) {
                        currencies.add(new CurrencyDTO(currencyCode, currencyName));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing currency JSON: {}", e.getMessage());
        }
        return currencies;
    }

    /**
     *  Helper function - Parses the JSON response and extracts exchange rates.
     */
    public Map<String, String> buildCurrencyMapping(JSONObject root) {
        Map<String, String> mapping = new HashMap<>();
        try {
            JSONObject data = root.getJSONObject(DATA);
            JSONObject structure = data.getJSONObject(STRUCTURE);
            JSONObject dimensions = structure.getJSONObject(DIMENSIONS);
            JSONArray seriesDimensions = dimensions.getJSONArray(SERIES);

            for (int i = 0; i < seriesDimensions.length(); i++) {
                JSONObject dimension = seriesDimensions.getJSONObject(i);
                if (STD_CURRENCY_KEY.equals(dimension.getString(ID))) {
                    JSONArray values = dimension.getJSONArray(VALUES);
                    for (int j = 0; j < values.length(); j++) {
                        JSONObject value = values.getJSONObject(j);
                        mapping.put(String.valueOf(j), value.getString(ID));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error building currency mapping: {}", e.getMessage());
        }
        return mapping;
    }

    /**
     * Parses the JSON response from the Bundesbank API and returns a list of ExchangeRateResponse objects.
     * this method uses the metadata mapping to return ISO codes.
     *
     * @param jsonResponse The JSON response as a string.
     * @return A list of ExchangeRateResponse objects.
     */
    public List<ExchangeRateResponse> parseExchangeRates(String jsonResponse) {
        List<ExchangeRateResponse> exchangeRates = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(jsonResponse);
            Map<String, String> currencyMapping = buildCurrencyMapping(root);

            JSONObject data = root.getJSONObject(DATA);
            JSONObject structure = data.getJSONObject(STRUCTURE);
            JSONObject dimensions = structure.getJSONObject(DIMENSIONS);
            JSONArray timePeriods = dimensions.getJSONArray(OBSERVATION)
                    .getJSONObject(0)
                    .getJSONArray(VALUES);

            JSONObject dataSet = data.getJSONArray(DATA_SETS).getJSONObject(0);
            JSONObject series = dataSet.getJSONObject(SERIES);

            for (String seriesKey : series.keySet()) {
                JSONObject seriesData = series.getJSONObject(seriesKey);
                if (!seriesData.has(OBSERVATIONS)) {
                    continue;
                }
                JSONObject observations = seriesData.getJSONObject(OBSERVATIONS);

                String[] keyParts = seriesKey.split(":");
                String numericCode = keyParts.length > 1 ? keyParts[1] : "UNKNOWN";
                String isoCurrencyCode = currencyMapping.getOrDefault(numericCode, numericCode);

                for (int i = 0; i < timePeriods.length(); i++) {
                    String date = timePeriods.getJSONObject(i).getString("id");
                    if (observations.has(String.valueOf(i))) {
                        JSONArray observationValues = observations.getJSONArray(String.valueOf(i));
                        if (!observationValues.isNull(0)) {
                            String rateStr = observationValues.getString(0);
                            try {
                                double rate = Double.parseDouble(rateStr);
                                exchangeRates.add(new ExchangeRateResponse(isoCurrencyCode, rate, date));
                            } catch (NumberFormatException e) {
                                logger.error("Invalid number format for date {}: {}", date, rateStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing exchange rates: {}", e.getMessage());
        }
        return exchangeRates;
    }

}
