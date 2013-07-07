package com.ideajack.touchboard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DefectWorkingThread extends Thread {

    private static final String LOG_TAG = "DefectWorkingThread";
    private static final String SERVER_TAG = "Yes, I am server.";
    private static final String CLIENT_DETECT_SERVER_TAG = "Are you a server.";
    private static Object lockObj = new Object();
    private static boolean sFoundServer = false;
    private static int sInterval = 0;
    private static int sLocalIpPart = 0;
    private static String sPrefixIP = null;
    private static int sTotalScannedIP = 0;
    private final int[] portRange = new int[] { 8123, 8124, 8125, 8126 };

    private int mStartIPPart = 0;
    private Handler mHandler = null;

    public DefectWorkingThread(int startPart, Handler handler) {
        mStartIPPart = startPart;
        mHandler = handler;
    }

    public static void initStaticMember(int interval, int localPart,
            String prefix) {
        sInterval = interval;
        sLocalIpPart = localPart;
        sPrefixIP = prefix;
    }

    @Override
    public void run() {

        int end = mStartIPPart + sInterval;
        if (end > 255)
            end = 255;
        for (int i = mStartIPPart; i < end; i++) {
            synchronized (lockObj) {
                if (sFoundServer)
                    break;
            }

            if (sLocalIpPart == i) {
                continue;
            }

            // new socket to connect to this ip
            String connectTo = sPrefixIP + i;
            for (int port : portRange) {
                Log.d(LOG_TAG,
                        "Trying to connect to "
                                + String.format("%s:%d", connectTo, port));
                if (isServer(connectTo, port)) {
                    synchronized (lockObj) {
                        Message msg = mHandler.obtainMessage(
                                TouchActivity.PC_SERVER_FOUND, port, 0,
                                connectTo);
                        msg.sendToTarget();
                        sFoundServer = true;
                        break;
                    }
                }
            } // end of for (int port : portRange) {
        } // end of for (int i = mStartIP; i < end; i++)

        synchronized (lockObj) {
            sTotalScannedIP += (end - mStartIPPart);
            // all ip scanned and not found server
            if (!sFoundServer && sTotalScannedIP >= 254) {
                Message msg = mHandler
                        .obtainMessage(TouchActivity.PC_SERVER_NOT_FOUND);
                msg.sendToTarget();
            }
        }
    } // end of public void run()

    public static boolean isServer(String ip, int port) {
        boolean serverFound = false;
        Socket socket = new Socket();
        SocketAddress remoteAddr = new InetSocketAddress(ip, port);
        try {
            socket.connect(remoteAddr, 1000);
        } catch (IOException ioEx) {
            Log.d(LOG_TAG,
                    "Exception when try connect: " + ioEx.getMessage());
            try {
                socket.close();
            } catch (IOException ioEx2) {
                Log.d(LOG_TAG,
                        "In isServer(...), exception occured when close socket.");
            }
            return serverFound;
        }
        if (socket.isConnected()) {
            try {
                // 1 send detect message to server
                BufferedOutputStream out = new BufferedOutputStream(
                        socket.getOutputStream());
                out.write(CLIENT_DETECT_SERVER_TAG.getBytes("UTF-8"));
                out.flush();

                // 2 receive response from server
                socket.setSoTimeout(300);
                BufferedInputStream in = new BufferedInputStream(
                        socket.getInputStream());
                byte[] lenBuffer = new byte[8];
                in.read(lenBuffer);
                long dataLen = ByteBuffer.wrap(lenBuffer).getLong();
                int serverTagLen = SERVER_TAG.getBytes("UTF-8").length;
                if (dataLen == serverTagLen) {
                    // read data bytes
                    byte[] dataBuf = new byte[serverTagLen];
                    in.read(dataBuf);
                    String readTag = new String(dataBuf, "UTF-8");
                    Log.d(LOG_TAG, "Data read: " + readTag);
                    if (readTag.equals(SERVER_TAG)) {
                        // ok, we find server
                        serverFound = true;
                    }
                }
            } catch (SocketException socketEx) {
                Log.d(LOG_TAG,
                        "Socket exceptoin: " + socketEx.getMessage());

            } catch (IOException ioEx) {
                Log.d(LOG_TAG, "IO exceptoin: " + ioEx.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ioEx) {
                    Log.d(LOG_TAG,
                            "In isServer(...), exception occured when close socket.");
                }
            }
        } // end of if (socket.isConnected())
        return serverFound;
    }
}
