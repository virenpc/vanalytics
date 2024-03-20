package com.example.demo.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) {
        // URL of the JSON data
        String url = "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";

        // Fetch JSON data from the URL
        String jsonData = fetchDataFromURL(url);

        if (jsonData != null) {
            // Parse JSON data and extract token-symbol pairs
            Map<String, String> tokenSymbolMap = parseJSONData(jsonData);

            // Print token-symbol pairs
            tokenSymbolMap.forEach((token, symbol) -> System.out.print("\""+token+"\":\""+symbol+"\","));
        } else {
            System.out.println("Failed to fetch JSON data from the URL.");
        }
    }

    private static String fetchDataFromURL(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(); // Print the exception stack trace
            return null;
        }
    }

    private static Map<String, String> parseJSONData(String jsonData) {
        Map<String, String> tokenSymbolMap = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonData);

            for (JsonNode node : rootNode) {
                JsonNode tokenNode = node.get("token");
                JsonNode symbolNode = node.get("name");
                JsonNode symbolName = node.get("symbol");
                JsonNode exch_seg = node.get("exch_seg");
                if (tokenNode != null && symbolNode != null && exch_seg!=null && symbolName!=null) {
                    String token = tokenNode.asText();
                    String symbol = symbolNode.asText();
                    String exString = exch_seg.asText();
                    String syName = symbolName.asText();
                    if (exString.equals("NSE") && syName.endsWith("-EQ"))
                        tokenSymbolMap.put(token, symbol);
                } else {
                    System.out.println("Missing required fields in JSON node.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print the exception stack trace
        }

        return tokenSymbolMap;
    }
}
