package com.example.bms;

import android.util.Log;

import com.bjw.ComAssistant.SerialHelper;
import com.bjw.bean.ComBean;

import java.util.Arrays;

import android_serialport_api.SerialPortFinder;
public class RFIDListener extends SerialHelper {

    private static final String TAG = "RFIDListener";
    private static final int BAUD_RATE = 19200;

    private OnRfidDataReceivedListener rfidDataReceivedListener;

    public RFIDListener(String port, OnRfidDataReceivedListener listener) {
        super(port, BAUD_RATE);
        this.rfidDataReceivedListener = listener;
    }

    public void setOnRfidDataReceivedListener(OnRfidDataReceivedListener listener) {
        this.rfidDataReceivedListener = listener;
    }

    @Override
    protected void onDataReceived(ComBean ComRecData) {
        // Print the received RFID data as hex and as string
        StringBuilder sb = new StringBuilder();
        sb.append("Received RFID data on port ").append(ComRecData.sComPort).append(": ");
        sb.append(bytesToHex(ComRecData.bRec));
        sb.append(" | As String: ").append(new String(ComRecData.bRec));
        sb.append(" | Actual Text: ").append(getPrintableText(ComRecData.bRec));
        sb.append(" | UTF-8: ").append(decodeWithCharset(ComRecData.bRec, "UTF-8"));
        sb.append(" | ISO-8859-1: ").append(decodeWithCharset(ComRecData.bRec, "ISO-8859-1"));
        sb.append(" | GBK: ").append(decodeWithCharset(ComRecData.bRec, "GBK"));
        sb.append(" | GB2312: ").append(decodeWithCharset(ComRecData.bRec, "GB2312"));
        sb.append(" | Big5: ").append(decodeWithCharset(ComRecData.bRec, "Big5"));
        sb.append(" |HexToString: ").append(hexToString(bytesToHex(ComRecData.bRec)));

        byte[] tagBytes = Arrays.copyOfRange(ComRecData.bRec, 4, 8);
        String tagHex = bytesToHex(tagBytes);
        sb.append(" | Tag Hex: ").append(tagHex);
        sb.append(" | Bytes ").append(Arrays.toString(ComRecData.bRec));

        Log.i(TAG, sb.toString());

        // Notify listener with Arrays.toString(ComRecData.bRec)
        if (rfidDataReceivedListener != null) {
            rfidDataReceivedListener.onRfidDataReceived(Arrays.toString(ComRecData.bRec));
        }
    }

    /**
     * Converts a byte array to a string containing only printable ASCII characters.
     */
    private String getPrintableText(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (byte b : data) {
            if (b >= 32 && b <= 126) { // printable ASCII range
                result.append((char) b);
            }
        }
        return result.toString();
    }

    /**
     * Decodes a byte array using the specified charset.
     */
    private static String decodeWithCharset(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (Exception e) {
            return "[Decode error: " + charsetName + "]";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Converts a hex string (e.g., "48656C6C6F") to its actual text (e.g., "Hello").
     */
    public static String hexToString(String hex) {
        if (hex == null || hex.length() == 0) return "";
        hex = hex.replaceAll("\\s", ""); // Remove spaces if any
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i+1), 16));
        }
        return new String(data);
    }

    /**
     * Starts listening for RFID card swipes on the first available serial device.
     * This method is deprecated in favor of using the constructor with a listener.
     */
    @Deprecated
    public static void startListening() {
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        String[] devicePaths = serialPortFinder.getAllDevicesPath();

        if (devicePaths.length == 0) {
            Log.e(TAG, "No serial devices found.");
            return;
        }

        Log.i(TAG, "Available Serial Devices:");
        for (String path : devicePaths) {
            Log.i(TAG, path);
        }

        String port = devicePaths[2];
        Log.i(TAG, "Listening for RFID on port: " + port + " at baud rate " + BAUD_RATE);

        // This static method is deprecated. Use the constructor with a listener instead.
        RFIDListener listener = new RFIDListener("/dev/ttyS3", null);
        try {
            listener.open();
            // Keep the thread alive to listen for data
            // In a real app, this would be managed by the Android lifecycle
        } catch (Exception e) {
            Log.e(TAG, "Failed to open serial port: " + e.getMessage());
        }
    }
}
