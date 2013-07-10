/**
 * 
 */
package com.ideajack.touchboard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * @author Chenwei
 *
 */
public class TimeMachine {
    private static final String LOG_TAG = "TimeMachine";
    private static final String TIME_TUNNEL_TAG = "Time Tunnel.";
    private static final String SERVER_RESP_OK = "OK";
    private Socket mTunnel = null;
    private String mIp = null;
    private int mPort = 0;
    private MachineHandler mMachineHandler = null;
    private static final int NEW_TUNNEL_SOCKET = 1;
    private static final int CLOSE_TUNNEL_SOCKET = 2;
    private static final int TIME_TUNNEL_TRANSMIT = 3;
    private static final int TRANSMIT_CTRL_TAG_DATA = 1;
    private static final int TRANSMIT_CTRL_TAG_QUIT = 0xFFFF;
    private Handler mProducer = null;
    private BufferedOutputStream out = null;
    private BufferedInputStream in = null;

    public TimeMachine(String ip, int port) {
        mIp = ip;
        mPort = port;
    }

    public void start(Handler producer) {
        Log.d(LOG_TAG, "Going to start TimeMachine.");
        mProducer = producer;
        HandlerThread thread = new HandlerThread("TimeMachine");
        thread.start();
        mMachineHandler = new MachineHandler(thread.getLooper());
        Message msg = mMachineHandler.obtainMessage(NEW_TUNNEL_SOCKET);
        msg.sendToTarget();
    }

    public void stop() {
        Log.d(LOG_TAG, "Going to stop TimeMachine.");
        mProducer = null;
        Message msg = mMachineHandler.obtainMessage(CLOSE_TUNNEL_SOCKET);
        msg.sendToTarget();
    }

    public void transmit(byte[] data) {
        if (data == null || data.length == 0)
            return;
        Message msg = mMachineHandler.obtainMessage(TIME_TUNNEL_TRANSMIT);
        msg.obj = data;
        msg.sendToTarget();
    }

    private final class MachineHandler extends Handler {
        public MachineHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NEW_TUNNEL_SOCKET:
                int what = EventTransfer.TIME_TUNNEL_CREATE_OK;
                if (!newTunnelSocket(mIp, mPort)) {
                    Log.d(LOG_TAG, "Can not new tunnel socket.");
                    what = EventTransfer.TIME_TUNNEL_CREATE_FAIL;
                }
                if (mProducer != null) {
                    Message msgRet = mProducer.obtainMessage(what);
                    msgRet.sendToTarget();
                } else {
                    Log.e(LOG_TAG, "mProducer is null, fatal error.");
                }
                break;
            case CLOSE_TUNNEL_SOCKET:
                closeTunnelSocket();
                mMachineHandler.getLooper().quit();
                mMachineHandler = null;
                break;
            case TIME_TUNNEL_TRANSMIT:
                byte[] data = (byte[]) msg.obj;
                doTransmit(data);
                break;
            default:
                break;
            }
        }
    }

    private boolean newTunnelSocket(String ip, int port) {
        // new time tunnel
        mTunnel = new Socket();
        SocketAddress remoteAddr = new InetSocketAddress(mIp, mPort);
        try {
            mTunnel.connect(remoteAddr, 1000);
            mTunnel.setKeepAlive(true);
        } catch (IOException ioEx) {
            Log.d(LOG_TAG, "Exception when try connect: " + ioEx.getMessage());
            try {
                mTunnel.close();
            } catch (IOException ioEx2) {
                Log.d(LOG_TAG,
                        "In isServer(...), exception occured when close socket.");
            }
            mTunnel = null;
            return false;
        }
        // send message to server that this is working socket and tell server to
        // hold it
        if (mTunnel.isConnected()) {
            try {
                // 1 send time tunnel tag to server
                out = new BufferedOutputStream(
                        mTunnel.getOutputStream());
                out.write(TIME_TUNNEL_TAG.getBytes("UTF-8"));
                out.flush();

                // 2 receive server response
                mTunnel.setSoTimeout(300);
                in = new BufferedInputStream(
                        mTunnel.getInputStream());
                byte[] revData = new byte[SERVER_RESP_OK.getBytes("UTF-8").length];
                in.read(revData);
                String resp = new String(revData, "UTF-8");
                if (resp.equals(SERVER_RESP_OK)) {
                    return true;
                }
            } catch (IOException ioEx) {
                Log.d(LOG_TAG,
                        "Exception when set up tunnel socket with server.");
                try {
                    mTunnel.close();
                } catch (IOException ioEx2) {
                    Log.d(LOG_TAG,
                            "Exception occured when try to close tunnel socket during consult with server.");
                }
                mTunnel = null;
            }
        }
        return false;
    }

    private void closeTunnelSocket() {
        if (mTunnel != null) {
            try {
                if (out != null) {
                    out.write(ByteBuffer.allocate(ICommonConstants.INT_BYTES)
                            .putInt(TRANSMIT_CTRL_TAG_QUIT).array());
                    out.flush();
                }
                mTunnel.close();
            } catch (IOException ioEx) {
                Log.d(LOG_TAG,
                        "Exception occured when try to close tunnel socket.");
            }
            mTunnel = null;
            in = null;
            out = null;
        }
    }

    private void doTransmit(byte[] data) {
        if (mTunnel == null || data == null || data.length == 0) {
            Log.d(LOG_TAG, "No need to transmission.");
            return;
        }

        if (out != null) {
            try {
                out.write(ByteBuffer.allocate(ICommonConstants.INT_BYTES)
                        .putInt(TRANSMIT_CTRL_TAG_DATA).array());
                byte[] totalLenBytes = ByteBuffer
                        .allocate(ICommonConstants.LONG_BYTES)
                        .putLong(data.length).array();
                out.write(totalLenBytes);
                out.write(data);
                out.flush();
            } catch (IOException ioEx) {
                Log.d(LOG_TAG,
                        "Exception occured when transmit data to server.");
            }
        }
    }
}
