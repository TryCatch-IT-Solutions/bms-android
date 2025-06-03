package com.bjw.ComAssistant;

import android.util.Log;

import com.bjw.bean.ComBean;

import android_serialport_api.SerialPortFinder;

/**
 * Listens for RFID card swipes on the first available serial device and prints the details.
 * Baud rate is set to 19200.
 */
public class RFIDListener extends SerialHelper {

    private static final String TAG = "RFIDListener";
    private static final int BAUD_RATE = 19200;

    public RFIDListener(String port) {
        super(port, BAUD_RATE);
    }

    @Override
    protected void onDataReceived(ComBean ComRecData) {
        // Print the received RFID data as hex and as string
        StringBuilder sb = new StringBuilder();
        sb.append("Received RFID data on port ").append(ComRecData.sComPort).append(": ");
        sb.append(bytesToHex(ComRecData.bRec));
        sb.append(" | As String: ").append(new String(ComRecData.bRec));
        Log.i(TAG, sb.toString());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Starts listening for RFID card swipes on the first available serial device.
     */
    public static void startListening() {
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        String[] devicePaths = serialPortFinder.getAllDevicesPath();

        if (devicePaths.length == 0) {
            Log.e(TAG, "No serial devices found.");
            return;
        }

        String port = devicePaths[0];
        Log.i(TAG, "Listening for RFID on port: " + port + " at baud rate " + BAUD_RATE);

        RFIDListener listener = new RFIDListener(port);
        try {
            listener.open();
            // Keep the thread alive to listen for data
            // In a real app, this would be managed by the Android lifecycle
        } catch (Exception e) {
            Log.e(TAG, "Failed to open serial port: " + e.getMessage());
        }
    }
}
