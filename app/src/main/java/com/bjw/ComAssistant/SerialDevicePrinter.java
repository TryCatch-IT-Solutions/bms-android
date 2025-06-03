package com.bjw.ComAssistant;

import android.util.Log;

import android_serialport_api.SerialPortFinder;

/**
 * Simplified class to list all available serial devices and print them.
 * Baud rate is set to 19200 (constant).
 */
public class SerialDevicePrinter {

    private static final String TAG = "SerialDevicePrinter";
    private static final int BAUD_RATE = 19200;

    public static void printAvailableDevices() {
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        String[] devicePaths = serialPortFinder.getAllDevicesPath();

        Log.i(TAG, "Baud Rate: " + BAUD_RATE);
        Log.i(TAG, "Available Serial Devices:");
        for (String path : devicePaths) {
            Log.i(TAG, path);
        }
    }
}
