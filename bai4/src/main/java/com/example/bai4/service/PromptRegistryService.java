package com.example.bai4.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.langfuse.client.LangfuseClient;
import com.langfuse.client.resources.prompts.requests.GetPromptRequest;
import com.langfuse.client.resources.prompts.types.Prompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PromptRegistryService {
    private final LangfuseClient langfuseClient;
    private final Cache<String, String> promptCache;

    // Tầng 3: Defensive Fallback Prompt
    private static final String DEFAULT_FALLBACK_PROMPT =
            "Bạn là trợ lý AI Banking chuyên nghiệp. Khách hàng: {{user_name}}. " +
                    "Số dư hiện tại: {{current_balance}}. Quy định: {{bank_policy}}. " +
                    "Hãy hỗ trợ khách hàng thực hiện giao dịch an toàn.";

    public PromptRegistryService(
            LangfuseClient langfuseClient,
            @Value("${cache.prompt.ttl-seconds:60}") long ttlSeconds,
            @Value("${cache.prompt.max-size:500}") long maxSize) {

        this.langfuseClient = langfuseClient;
        this.promptCache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .build();
    }

    /**
     * Lấy prompt: Tầng 2 (Cache) -> Tầng 1 (Remote Fetch) -> Tầng 3 (Fallback) -> Tầng 4 (Compile)
     */
    public String getPrompt(String promptName, Map<String, Object> variables) {
        long startTime = System.currentTimeMillis();

        // Tầng 2: In-Memory Cache Lookup
        String rawTemplate = promptCache.getIfPresent(promptName);

        if (rawTemplate != null) {
            long latency = System.currentTimeMillis() - startTime;
            log.info("[CACHE HIT] Loaded prompt '{}' from local cache in {} ms", promptName, latency);
        } else {
            // Tầng 1: Remote Fetch & Tầng 3: Defensive Fallback
            log.info("[CACHE MISS] Fetching prompt '{}' from Langfuse Server...", promptName);
            try {
                GetPromptRequest request = GetPromptRequest.builder()
                        .label("production")
                        .build();

                Prompt prompt = langfuseClient.prompts().get(promptName, request);
                rawTemplate = extractPromptText(prompt);

                // Lưu vào Caffeine Cache
                promptCache.put(promptName, rawTemplate);
                long latency = System.currentTimeMillis() - startTime;
                log.info("[FETCH SUCCESS] Successfully fetched prompt '{}' from Langfuse in {} ms", promptName, latency);
            } catch (Exception ex) {
                log.warn("[FALLBACK TRIGGERED] Failed to fetch prompt '{}' from Langfuse: {}. Switching to default fallback prompt.",
                        promptName, ex.getMessage());
                rawTemplate = DEFAULT_FALLBACK_PROMPT;
            }
        }

        // Tầng 4: Compile Variables
        return compileVariables(rawTemplate, variables);
    }

    /**
     * Trích xuất chuỗi prompt từ đối tượng Prompt SDK
     */
    private String extractPromptText(Prompt prompt) {
        if (prompt == null) {
            return DEFAULT_FALLBACK_PROMPT;
        }

        try {
            if (prompt.isText() && prompt.getText().isPresent()) {
                return prompt.getText().get().getPrompt();
            }
            if (prompt.isChat() && prompt.getChat().isPresent()) {
                return prompt.getChat().get().toString();
            }
        } catch (Exception e) {
            log.debug("Using toString extraction fallback: {}", e.getMessage());
        }

        return prompt.toString();
    }

    /**
     * Thay thế placeholder {{variable}} bằng giá trị thực
     */
    private String compileVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}