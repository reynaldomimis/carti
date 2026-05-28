package com.upreyvan.carti.data.ai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.upreyvan.carti.R;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.util.concurrent.Executors;

public class AiTestActivity extends AppCompatActivity {

    private TextView tvLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_test);

        tvLogs = findViewById(R.id.tvLogs);
        Button btnRunTest = findViewById(R.id.btnRunTest);

        btnRunTest.setOnClickListener(v -> runTest());
    }

    private void runTest() {
        tvLogs.setText("Starting AI Test...\n");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                runOnUiThread(() -> tvLogs.append("Building Client...\n"));
                
                Client client = Client.builder()
                    .apiKey("AIzaSyBFuJN04vrCld77VZjf-4QX_zuzTSpJ7cE")
                    .build();

                runOnUiThread(() -> tvLogs.append("Client built. Sending request (gemini-3.5-flash)...\n"));

                GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.5-flash",
                    "What are some smart ways to save ₱5,000 this month for my family budget?",
                    null
                );

                String result = response.text();
                runOnUiThread(() -> {
                    tvLogs.append("\n--- AI RESPONSE START ---\n");
                    tvLogs.append(result);
                    tvLogs.append("\n--- AI RESPONSE END ---\n");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvLogs.append("\nTEST FAILED!\n");
                    tvLogs.append(e.getMessage());
                });
            }
        });
    }
}
