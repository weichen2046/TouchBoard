package com.ideajack.touchboard.utils;

public class BytesTools {

    public static final String ConvertArrayToString(byte[] data) {

        if (data == null)
            return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            builder.append(String.format("%02X ", data[i]));
        }

        return builder.toString();
    }
}
