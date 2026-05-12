package com.uniovi.estimacion.selenium.pageobjects;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PO_Properties {

    public static final Locale SPANISH = Locale.ROOT;
    public static final Locale ENGLISH = Locale.ENGLISH;

    private static final String BUNDLE_NAME = "messages";

    public String getString(String key) {
        return getString(key, SPANISH);
    }

    public String getString(String key, Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        } catch (MissingResourceException exception) {
            return "??" + key + "??";
        }
    }

    public String getString(String key, Locale locale, Object... args) {
        String message = getString(key, locale);
        return MessageFormat.format(message, args);
    }
}