package com.upreyvan.carti.data.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class AiTest {
    public static void main(String[] args) {
        try {
            System.out.println("Starting AI Test...");
            
            // DIRECT API KEY
            Client client = Client.builder()
                .apiKey("AIzaSyBFuJN04vrCld77VZjf-4QX_zuzTSpJ7cE")
                .build();

            System.out.println("Client built. Sending request...");

            // Gamitin natin ang stable model muna para sigurado
            GenerateContentResponse response = client.models.generateContent(
                "gemini-1.5-flash",
                "Hello, this is a test from the Carti app. Are you active?",
                null
            );

            System.out.println("--- AI RESPONSE START ---");
            System.out.println(response.text());
            System.out.println("--- AI RESPONSE END ---");

        } catch (Exception e) {
            System.err.println("TEST FAILED!");
            e.printStackTrace();
        }
    }
}
