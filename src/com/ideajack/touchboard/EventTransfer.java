package com.ideajack.touchboard;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

public class EventTransfer {
    private static final String LOG_TAG = "EventTransfer";
    public static final int MOTION_EVENT = 1;
    public static final int TIME_TUNNEL_CREATE_OK = 2;
    public static final int TIME_TUNNEL_CREATE_FAIL = 3;
    private WorkingHandler mWorkingHandler = null;
    private TimeMachine mTimeMachine = null;

    public EventTransfer() {
        HandlerThread thread = new HandlerThread("MotionEventWokingHandler");
        thread.start();
        mWorkingHandler = new WorkingHandler(thread.getLooper());
    }

    public void startWork(String ip, int port) {
        // create time machine
        mTimeMachine = new TimeMachine(ip, port);
        mTimeMachine.start(mWorkingHandler);
    }

    public void stopWork() {
        if (mTimeMachine != null) {
            mTimeMachine.stop();
            mTimeMachine = null;
        }
        mWorkingHandler.getLooper().quit();
        mWorkingHandler = null;
    }

    public void sendEvent(int id, Object param) {
        Message msg = mWorkingHandler.obtainMessage(id);
        msg.obj = param;
        msg.sendToTarget();
    }

    private final class WorkingHandler extends Handler {
        public WorkingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MOTION_EVENT:
                // Log.d(LOG_TAG, "MotionEventHandler, motion event arrived.");
                MotionEvent event = (MotionEvent) msg.obj;
                handleMotionEvent(event);
                break;
            case TIME_TUNNEL_CREATE_OK:
                break;
            case TIME_TUNNEL_CREATE_FAIL:
                mTimeMachine = null;
                break;
            default:
                Log.d(LOG_TAG, "MotionEventHandler, default event arrived.");
            }
        }
    }

    private byte[] serializeMotionEvent(MotionEvent event) {
        int temp = 0;
        List<Byte> result = new ArrayList<Byte>();
        // add action, 4 bytes
        addAllBytes(result, ByteBuffer.allocate(4).putInt(event.getAction())
                .array());
        // add pointer count, 4 bytes
        temp = event.getPointerCount();
        addAllBytes(result, ByteBuffer.allocate(4).putInt(temp).array());

        return byteListToArray(result);
    }

    private void addAllBytes(List<Byte> holder, byte[] data) {
        if (holder != null && data != null) {
            for (int i = 0; i < data.length; i++) {
                holder.add(data[i]);
            }
        }
    }

    private byte[] byteListToArray(List<Byte> holder) {
        byte[] result = null;
        if (holder != null && holder.size() != 0) {
            result = new byte[holder.size()];
            for (int i = 0; i < holder.size(); i++) {
                result[i] = holder.get(i);
            }
        }
        return result;
    }

    private void handleMotionEvent(MotionEvent event) {
        // Log.d(LOG_TAG,
        // "Going to handle motion event, action: " + event.getAction());
        if (mTimeMachine == null)
            return;
        switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
            Log.d(LOG_TAG, "ACTION_MOVE: " + "X: " + event.getX() + " Y: "
                    + event.getY() + " Point Count: " + event.getPointerCount());
            break;
        case MotionEvent.ACTION_DOWN:
            Log.d(LOG_TAG, "ACTION_DOWN: " + "X: " + event.getX() + " Y: "
                    + event.getY() + " Point Count: " + event.getPointerCount());
            break;
        case MotionEvent.ACTION_UP:
            Log.d(LOG_TAG, "ACTION_UP: " + "X: " + event.getX() + " Y: "
                    + event.getY() + " Point Count: " + event.getPointerCount());
            break;
        case MotionEvent.ACTION_POINTER_UP:
            Log.d(LOG_TAG,
                    "ACTION_POINTER_UP: " + "X: " + event.getX() + " Y: "
                            + event.getY() + " Point Count: "
                            + event.getPointerCount());
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            Log.d(LOG_TAG,
                    "ACTION_POINTER_DOWN: " + "X: " + event.getX() + " Y: "
                            + event.getY() + " Point Count: "
                            + event.getPointerCount());
            break;
        case MotionEvent.ACTION_CANCEL:
            Log.d(LOG_TAG, "ACTION_CANCEL: " + "X: " + event.getX() + " Y: "
                    + event.getY() + " Point Count: " + event.getPointerCount());
            break;
        default:
            Log.d(LOG_TAG, "Default action: " + event.getAction() + " X: "
                    + event.getX() + " Y: " + event.getY() + " Point Count: "
                    + event.getPointerCount());
            break;
        }
        mTimeMachine.transmit(serializeMotionEvent(event));
    }
}
