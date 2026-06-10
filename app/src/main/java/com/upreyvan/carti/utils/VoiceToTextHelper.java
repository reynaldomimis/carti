package com.upreyvan.carti.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceToTextHelper {
    private static final String TAG = "VoiceToTextHelper";
    private final SpeechRecognizer speechRecognizer;
    private final Intent recognizerIntent;
    private final VoiceCallback callback;
    private boolean isListening = false;

    public interface VoiceCallback {
        void onResult(String text);
        void onError(String error);
        void onPartialResult(String text);
        default void onReadyForSpeech() {}
        default void onBeginningOfSpeech() {}
        default void onEndOfSpeech() {}
    }

    public VoiceToTextHelper(Context context, VoiceCallback callback) {
        this.callback = callback;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.getApplicationContext());
        this.recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        setupListener();
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

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void setupListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { if (callback != null) callback.onReadyForSpeech(); }
            @Override public void onBeginningOfSpeech() { if (callback != null) callback.onBeginningOfSpeech(); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { 
                isListening = false; 
                if (callback != null) callback.onEndOfSpeech(); 
            }
            @Override public void onError(int error) {
                isListening = false;
                String message = switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO -> "Audio recording error";
                    case SpeechRecognizer.ERROR_CLIENT -> "Client side error";
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions";
                    case SpeechRecognizer.ERROR_NETWORK -> "Network error";
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout";
                    case SpeechRecognizer.ERROR_NO_MATCH -> "No match found";
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy";
                    case SpeechRecognizer.ERROR_SERVER -> "Server error";
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input";
                    default -> "Unknown error";
                };
                Log.e(TAG, "Error: " + message);
                if (callback != null) callback.onError(message);
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onResult(matches.get(0));
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onPartialResult(matches.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }
}
