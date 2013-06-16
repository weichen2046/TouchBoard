package com.ideajack.touchboard;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TouchActivity extends Activity {

    private static final String LOG_TAG = "TouchActivity";
    private static final int GET_WIFI_IP_ADDRESS_OK = 1;
    private static final int GET_WIFI_IP_ADDRESS_FAIL = 2;
    private static final int WIFI_NOT_CONNECTED = 3;
    public static final int PC_SERVER_FOUND = 4;
    public static final int PC_SERVER_NOT_FOUND = 5;
    private String mIpAddress = null;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_touch);

        new Thread(mInitRunnable).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d(LOG_TAG, "X: " + event.getX() + " Y: " + event.getY());
        }

        return super.onTouchEvent(event);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WIFI_NOT_CONNECTED:
                Toast.makeText(TouchActivity.this, R.string.wifi_not_connected,
                        Toast.LENGTH_LONG).show();
                break;
            case GET_WIFI_IP_ADDRESS_OK:
                mIpAddress = (String) msg.obj;
                Toast.makeText(TouchActivity.this, "IP Address: " + mIpAddress,
                        Toast.LENGTH_LONG).show();
                // show a progress dialog
                progressDialog = ProgressDialog.show(TouchActivity.this,
                        "Loading...", "Please wait...", true, false);
                new DetectPCServerThread(this, mIpAddress).start();
                break;
            case GET_WIFI_IP_ADDRESS_FAIL:
                Toast.makeText(TouchActivity.this,
                        R.string.get_wifi_ip_address_fail, Toast.LENGTH_LONG)
                        .show();
                mIpAddress = null;
                break;
            case PC_SERVER_NOT_FOUND:
                progressDialog.dismiss();
                Toast.makeText(TouchActivity.this,
                        R.string.pc_server_not_found, Toast.LENGTH_LONG)
                        .show();
                break;
            case PC_SERVER_FOUND:
                progressDialog.dismiss();
                Toast.makeText(TouchActivity.this,
                        String.format(
                                getResources().getString(
                                        R.string.pc_server_found),
                                msg.obj,
                                msg.arg1),
                                Toast.LENGTH_LONG)
                                .show();
                break;
            }
            super.handleMessage(msg);
        }
    };

    private final Runnable mInitRunnable = new Runnable() {
        @Override
        public void run() {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected()) {
                // get wifi ip address
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                byte[] bytes = BigInteger.valueOf(ipAddress).toByteArray();
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    // reverse the byte order
                    for (int i = 0; i < bytes.length / 2; i++) {
                        byte temp = bytes[i];
                        bytes[i] = bytes[bytes.length - 1 - i];
                        bytes[bytes.length - 1 - i] = temp;
                    }
                }
                try {
                    Inet4Address address = (Inet4Address) Inet4Address
                            .getByAddress(bytes);
                    Log.d(LOG_TAG, "IP address: " + address.getHostAddress());
                    Message msg = mHandler.obtainMessage(
                            GET_WIFI_IP_ADDRESS_OK, address.getHostAddress());
                    msg.sendToTarget();
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Message msg = mHandler
                            .obtainMessage(GET_WIFI_IP_ADDRESS_FAIL);
                    msg.sendToTarget();
                }
            } else {
                Message msg = mHandler.obtainMessage(WIFI_NOT_CONNECTED);
                msg.sendToTarget();
            }
        }
    };
}
