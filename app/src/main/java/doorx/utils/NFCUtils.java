package doorx.utils;

import android.content.Intent;
import android.hibory.Conversion;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.Locale;

public class NFCUtils {
    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    public static String processNfcIntent(Intent intent) {
        //if (!intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) return "--";

        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tagFromIntent==null){
            return "null";
        }
        StringBuffer data = new StringBuffer();
        //data.append("NFC读卡数据：\n");
        data.append("ID：");
        data.append(Conversion.Bytes2HexString(tagFromIntent.getId()));
        data.append("\n Tech List：\n");
        for (String tech : tagFromIntent.getTechList()) {
            data.append(tech + "\n");
        }
        return data.toString();


    }

    //初次判断是什么类型的NFC卡
    public static NdefMessage[] getNdefMsg(Intent intent) {
        if (intent == null)
            return null;

        //nfc卡支持的格式
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] temp = tag.getTechList();
        for (String s : temp) {
            Log.d("hello", "resolveIntent tag: " + s);
        }


        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Parcelable[] rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] ndefMessages = new NdefMessage[0];

            // 判断是哪种类型的数据 默认为NDEF格式
            if (rawMessage != null) {
                Log.d("hello", "getNdefMsg: ndef格式 ");
                ndefMessages = new NdefMessage[rawMessage.length];
                for (int i = 0; i < rawMessage.length; i++) {
                    ndefMessages[i] = (NdefMessage) rawMessage[i];
                }
            } else {
                //未知类型 (公交卡类型)
               // Log.i(TAG, "getNdefMsg: 未知类型");
                //对应的解析操作，在Github上有
            }


            return ndefMessages;
        }

        return null;
    }


    /**
     * 创建一个封装要写入的文本的NdefRecord对象
     *
     * @param text
     * @return
     */
    public NdefRecord createTextRecord(String text) {
        // 生成语言编码的字节数组，中文编码
        byte[] langBytes = Locale.US.getLanguage().getBytes(Charset.forName("US-ASCII"));
        // 将要写入的文本以UTF_8格式进行编码
        Charset utfEncoding = Charset.forName("UTF-8");
        // 由于已经确定文本的格式编码为UTF_8，所以直接将payload的第1个字节的第7位设为0
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = 0;
        // 定义和初始化状态字节
        char status = (char) (utfBit + langBytes.length);
        // 创建存储payload的字节数组
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        // 设置状态字节
        data[0] = (byte) status;
        // 设置语言编码
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        // 设置实际要写入的文本
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        // 根据前面设置的payload创建NdefRecord对象
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }

    /**
     * 开始写入信息
     *
     * @param intent
     * @param ndefMessage
     */
    private void startWriteMsg(Intent intent, NdefMessage ndefMessage) {
        final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    //Tools.showTip(NFCActivity.this, "标签是只读！");
                    //NFCActivity.this.onBackPressed();
                    //NFCActivity.this.finish();
                    return;
                }
                int size = ndefMessage.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    //Tools.showTip(NFCActivity.this, "空间不足！");
                    //NFCActivity.this.onBackPressed();
                    //NFCActivity.this.finish();
                    return;
                }
                ndef.writeNdefMessage(ndefMessage);
                // Tools.showTip(NFCActivity.this, "写入成功");// 写入成功
                // TODO 写入成功,操作UI
                return;
            } else {
                // 获取可以格式化和向标签写入数据NdefFormatable对象
                NdefFormatable format = NdefFormatable.get(tag);
                // 向非NDEF格式或未格式化的标签写入NDEF格式数据
                if (format != null) {
                    try {
                        // 允许对标签进行IO操作
                        format.connect();
                        format.format(ndefMessage);
                        //Tools.showTip(NFCActivity.this, "写入成功");// 写入成功
                        // TODO 写入成功,操作UI
                        return;
                    } catch (Exception e) {
                        // Tools.showTip(NFCActivity.this, "写入失败");
                        // TODO 写入失败 操作UI
                        return;
                    }
                } else {
                    //Tools.showTip(NFCActivity.this, "NFC标签不支持NDEF格式");
                    // TODO NFC标签不支持NDEF格式！ 操作UI
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
