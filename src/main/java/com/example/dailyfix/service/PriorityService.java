package com.example.dailyfix.service;

import com.example.dailyfix.enums.*;
import com.example.dailyfix.model.Message;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@Service
public class PriorityService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private final RestTemplate restTemplate = new RestTemplate();

    // FIXED: Now accepts TrustLevel as the second argument
    public Priority calculatePriority(Message message, TrustLevel trust) {
        try {
            // 1. Sanitize input to prevent HTML tags from confusing the AI
            String cleanSubject = HtmlUtils.htmlUnescape(message.getSubject());
            String cleanContent = HtmlUtils.htmlUnescape(message.getContent());

            // 2. Industry-Grade System Prompt (Persona + Heuristics + Examples)
            String prompt = String.format("""
            # PERSONA: Senior Executive Assistant
            # GOAL: Triage emails for a high-priority software lead.
            # SENDER TRUST: %s (High trust = verify requests; Low trust = be skeptical).

            # CATEGORIZATION RULES:
            - HIGH: System failures, production issues, deadlines < 24h, or direct boss requests.
            - MEDIUM: Project updates, internal follow-ups, or standard client meetings.
            - LOW: Newsletters, general industry updates, or non-urgent internal FYIs.
            - SILENT: Automated system notifications (Social media, generic Google alerts).

            # EXAMPLES:
            - "Urgent: Database resize needed in 2h" -> {"priority": "HIGH", "intent": "ACTION_REQUIRED"}
            - "Your weekly GitHub report" -> {"priority": "SILENT", "intent": "INFORMATIONAL"}

            # CURRENT EMAIL:
            Subject: %s
            Content: %s

            Return JSON ONLY: {"priority": "HIGH|MEDIUM|LOW|SILENT", "intent": "ACTION_REQUIRED|INFORMATIONAL|PROMOTIONAL", "reason": "why"}
            """, trust.name(), cleanSubject, cleanContent);

            Map<String, Object> body = Map.of("contents", new Object[]{
                    Map.of("parts", new Object[]{Map.of("text", prompt)})
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // CORRECTED: Explicitly declaring the 'response' variable here
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL + apiKey, entity, String.class);

            if (response.getBody() == null) {
                System.err.println("❌ AI Error: Empty Response Body");
                return Priority.SILENT;
            }

            // 3. Robust JSON Extraction
            JSONObject json = new JSONObject(response.getBody());
            String rawText = json.getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                    .getString("text");

            // Remove markdown formatting (```json ... ```) if Gemini returns it
            String cleanJson = rawText.replaceAll("(?s)^.*?\\{", "{").replaceAll("\\}.*?$", "}");
            JSONObject result = new JSONObject(cleanJson);

            message.setIntent(MessageIntent.valueOf(result.getString("intent")));
            System.out.println("AI Decision Reason: " + result.getString("reason"));

            return Priority.valueOf(result.getString("priority"));

        } catch (Exception e) {
            System.err.println("Gemini analysis failed: " + e.getMessage());
            return Priority.SILENT;
        }
    }
}