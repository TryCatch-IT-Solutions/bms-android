package com.example.bms.utils;

import android.util.Log;

/**
 * Centralized logging utility for BMS application.
 *
 * TO DISABLE ALL LOGS: Change ENABLE_LOGGING to false
 *
 * Usage:
 *   Instead of: Log.d("Tag", "message");
 *   Use:        Logger.d("Tag", "message");
 */
public class Logger {

    /**
     * Master switch for all logging in the application.
     *
     * CHANGE THIS TO false TO DISABLE ALL LOGS
     */
    private static final boolean ENABLE_LOGGING = false;

    /**
     * Log levels can be individually controlled
     */
    private static final boolean ENABLE_DEBUG = true;
    private static final boolean ENABLE_INFO = true;
    private static final boolean ENABLE_WARNING = true;
    private static final boolean ENABLE_ERROR = true;

    /**
     * Debug log
     */
    public static void d(String tag, String message) {
        if (ENABLE_LOGGING && ENABLE_DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * Info log
     */
    public static void i(String tag, String message) {
        if (ENABLE_LOGGING && ENABLE_INFO) {
            Log.i(tag, message);
        }
    }

    /**
     * Warning log
     */
    public static void w(String tag, String message) {
        if (ENABLE_LOGGING && ENABLE_WARNING) {
            Log.w(tag, message);
        }
    }

    /**
     * Error log
     */
    public static void e(String tag, String message) {
        if (ENABLE_LOGGING && ENABLE_ERROR) {
            Log.e(tag, message);
        }
    }

    /**
     * Error log with exception
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (ENABLE_LOGGING && ENABLE_ERROR) {
            Log.e(tag, message, throwable);
        }
    }

    /**
     * Verbose log
     */
    public static void v(String tag, String message) {
        if (ENABLE_LOGGING) {
            Log.v(tag, message);
        }
    }

    /**
     * Check if logging is enabled
     */
    public static boolean isLoggingEnabled() {
        return ENABLE_LOGGING;
    }
}
