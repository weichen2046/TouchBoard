package com.ideajack.touchboard.utils;

import java.util.List;

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

    public static void addAllBytes(List<Byte> holder, byte[] data) {
        if (holder != null && data != null) {
            for (int i = 0; i < data.length; i++) {
                holder.add(data[i]);
            }
        }
    }

    public static byte[] byteListToArray(List<Byte> holder) {
        byte[] result = null;
        if (holder != null && holder.size() != 0) {
            result = new byte[holder.size()];
            for (int i = 0; i < holder.size(); i++) {
                result[i] = holder.get(i);
            }
        }
        return result;
    }
}
