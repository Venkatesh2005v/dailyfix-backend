package com.example.dailyfix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;

@Service
public class AIService {

    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.builder().build();

    public String summarizeEmails(String rawContent) {
        String prompt = "Review these separate email threads from the last 72 hours. " +
                "Provide a ONE-LINE situational report (max 20 words) that identifies " +
                "key participants or topics. " +
                "Example: 'Updates from HR regarding payroll and a request from Team Alpha on the API.' " +
                "Data: " + rawContent;
        return callGemini(prompt);
    }

    public String summarizeTasks(String taskContent) {
        String prompt = "Review these active tasks. Provide a ONE-LINE executive summary " +
                "(max 15 words) describing the user's current primary work focus. " +
                "Tasks: " + taskContent;
        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            );

            Map<String, Object> response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey) // Ensure apiUrl does NOT have a ? already
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, resp) -> {
                        System.out.println("Gemini API Error: " + resp.getStatusCode());
                    })
                    .body(Map.class);

            if (response == null || !response.containsKey("candidates")) {
                return "Intelligence gathering... No insights found.";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates.isEmpty()) return "No situational updates available.";

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            return parts.get(0).get("text").toString().trim();

        } catch (Exception e) {
            System.err.println("Gemini Integration Failed: " + e.getMessage());
            e.printStackTrace();
            return "Intelligence offline. Manual review required.";
        }
    }
}