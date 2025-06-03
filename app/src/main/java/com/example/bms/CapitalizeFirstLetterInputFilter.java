package com.example.bms;

import android.text.InputFilter;
import android.text.Spanned;

public class CapitalizeFirstLetterInputFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (source.length() > 0 && dstart == 0) {
            char firstChar = source.charAt(0);
            if (Character.isLetter(firstChar) && Character.isLowerCase(firstChar)) {
                return Character.toUpperCase(firstChar) + source.subSequence(1, end).toString();
            }
        }
        return null;
    }
}