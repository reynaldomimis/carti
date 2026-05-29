package com.upreyvan.carti.data.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * ELITE SENIOR LEVEL: Centralized Voice-to-Text Helper.
 * Handles Android SpeechRecognizer with a clean callback architecture.
 */
public class VoiceToTextHelper {

    private static final String TAG = "VoiceToTextHelper";
    private static volatile VoiceToTextHelper instance;

    private final SpeechRecognizer speechRecognizer;
    private final Intent recognizerIntent;
    private VoiceCallback callback;
    private boolean isListening = false;

    public interface VoiceCallback {
        void onReadyForSpeech();
        void onBeginningOfSpeech();
        void onRmsChanged(float rmsdB);
        void onBufferReceived(byte[] buffer);
        void onEndOfSpeech();
        void onError(String error);
        void onResults(String text);
        void onPartialResults(String partialText);
    }

    private VoiceToTextHelper(Context context) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.getApplicationContext());
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        
        // Tagalog support if needed, but defaults to system locale
        // recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fil-PH");

        setupListener();
    }

    public static VoiceToTextHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (VoiceToTextHelper.class) {
                if (instance == null) {
                    instance = new VoiceToTextHelper(context);
                }
            }
        }
        return instance;
    }

    public void setCallback(VoiceCallback callback) {
        this.callback = callback;
    }

    public void startListening() {
        if (!isListening) {
            speechRecognizer.startListening(recognizerIntent);
            isListening = true;
        }
    }

    public void stopListening() {
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        instance = null;
    }

    private void setupListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                if (callback != null) callback.onReadyForSpeech();
            }

            @Override
            public void onBeginningOfSpeech() {
                if (callback != null) callback.onBeginningOfSpeech();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (callback != null) callback.onRmsChanged(rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                if (callback != null) callback.onBufferReceived(buffer);
            }

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                if (callback != null) callback.onEndOfSpeech();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                String message = getErrorText(error);
                Log.e(TAG, "Error: " + message);
                if (callback != null) callback.onError(message);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (callback != null) callback.onResults(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (callback != null) callback.onPartialResults(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Unknown error";
        }
    }
}
