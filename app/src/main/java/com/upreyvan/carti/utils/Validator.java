package com.upreyvan.carti.utils;

import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;

public class Validator {

    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isEmpty(EditText editText) {
        return editText == null || isEmpty(editText.getText().toString());
    }

    public static boolean isValidEmail(String email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return !isEmpty(password) && password.length() >= 8;
    }

    public static boolean areNotEmpty(String... strings) {
        for (String s : strings) {
            if (isEmpty(s)) return false;
        }
        return true;
    }
}
