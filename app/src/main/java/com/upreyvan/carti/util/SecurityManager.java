package com.upreyvan.carti.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityManager {

    private static final String ENCRYPTED_PREFS_NAME = "carti_secure_prefs";

    public static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback to regular prefs if encryption fails (not ideal, but prevents crash)
            return context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Basic runtime obfuscation to make strings harder to find in memory dumps.
     */
    public static String obfuscate(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append((char) (input.charAt(i) ^ 0x42));
        }
        return sb.toString();
    }

    public static String deobfuscate(String input) {
        return obfuscate(input); // XOR is symmetric
    }
}
