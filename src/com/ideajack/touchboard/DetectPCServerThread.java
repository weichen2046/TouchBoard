package com.ideajack.touchboard;

import android.os.Handler;
import android.util.Log;

public class DetectPCServerThread extends Thread {

    private static final String LOG_TAG = "DetectPCServerThread";

    private Handler mHandler = null;
    private String mPhoneIpAddress = null;
    private static final int MAX_SCAN_THREAD = 10;

    public DetectPCServerThread(Handler handler, String ip) {
        mHandler = handler;
        mPhoneIpAddress = ip;
    }

    @Override
    public void run() {
        Log.d(LOG_TAG, "Going to detect server, and local ip: "
                + mPhoneIpAddress);
        String prefix = mPhoneIpAddress.substring(0,
                mPhoneIpAddress.lastIndexOf('.') + 1);

        String lastPart = mPhoneIpAddress.split("\\.")[3];
        int localPart = Integer.parseInt(lastPart);

        int interval = (255 / MAX_SCAN_THREAD) + 1;

        DefectWorkingThread.initStaticMember(interval, localPart, prefix);

        for (int i = 0; i < MAX_SCAN_THREAD; i++) {
            int startPart = i*interval + 1;
            new DefectWorkingThread(startPart, mHandler).start();
        }
    }
}
