package com.example.bai4;

import com.example.bai4.service.PromptRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankingTestRunner implements CommandLineRunner {
    private final PromptRegistryService promptRegistryService;

    @Override
    public void run(String... args) throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_name", "Nguyen Van A");
        variables.put("current_balance", "50,000,000 VND");
        variables.put("bank_policy", "Xác thực OTP cho giao dịch trên 10,000,000 VND");

        log.info("=== TEST RUN: LANGFUSE PROMPT REGISTRY ===");

        log.info("--- LẦN GỌI 1 ---");
        String prompt1 = promptRegistryService.getPrompt("banking_transfer_prompt", variables);
        log.info("Prompt Kết quả 1:\n{}", prompt1);

        log.info("--- LẦN GỌI 2 (Kiểm tra Hit Cache 0ms) ---");
        String prompt2 = promptRegistryService.getPrompt("banking_transfer_prompt", variables);
        log.info("Prompt Kết quả 2:\n{}", prompt2);
    }
}