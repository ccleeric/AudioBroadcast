package com.ccleeric.audiobroadcast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by ccleeric on 13/9/25.
 */
public class BluetoothManager implements Notifier {

    public static final int ACTION_CONNECT_LOST   = 0x101;
    public static final int ACTION_CONNECT_FAILED = 0x102;

    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    public static final String NAME_SECURE = "BluetoothAudioSecure";

    // Unique UUID for this application
    public static final UUID AUDIO_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private static final String TAG = "Bluetooth Manager";

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private BroadcastController mController;
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mSocket;

    //Bluetooth State
    private ListenThread mListenThread;
    private ConnectingThread mConnectingThread;
    private int mState;

    private InputStream mAudioInStream;
    private OutputStream mAudioOutStream;


    public BluetoothManager(BroadcastController controller) {
        mController = controller;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mSocket = null;
    }

    public BluetoothAdapter getAdapter() {
        return mBtAdapter;
    }

    public int getState() {
        return mState;
    }

    public OutputStream getAudioOutStream() {
        return mAudioOutStream;
    }

    public void initAudioStreams(BluetoothSocket socket) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        mAudioInStream = tmpIn;
        mAudioOutStream = tmpOut;
    }

    public void doDiscovery() {

        // If we're already discovering, stopAudio it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    public void connectLost() {
        listen();
    }

    public void connectFailed() {
        String msg  =  "Unable to connect device";
        update(BluetoothManager.ACTION_CONNECT_FAILED, msg);

        //Bluetooth enter listening mode
        listen();
    }

    public synchronized void listen() {
        if(mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }

//        if(mSocket != null) {
//            disconnect();
//        }

        mState = STATE_LISTEN;
        if(mListenThread == null) {
            mListenThread = new ListenThread(this);
            mListenThread.start();
        }
    }

    public synchronized void finishConnecting() {
        mConnectingThread = null;
    }

    public synchronized void connected(BluetoothSocket socket) {

        if(mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }

        if(mListenThread != null) {
            mListenThread.closeSocket();
            mListenThread = null;
        }

        mSocket = socket;
        initAudioStreams(socket);
        //notifyObserver(AudioPlayer.ACTION_PLAY, mAudioInStream);
        update(AudioPlayer.ACTION_PLAY, mAudioInStream);
        mState = STATE_CONNECTED;
    }

    public synchronized void connect(String address) {
        Log.d(TAG, "1111 connect function in Manager");
        if(mState == STATE_CONNECTING) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }

        mConnectingThread = new ConnectingThread(this, address);
        mConnectingThread.start();

        mState = STATE_CONNECTING;
    }

    public void disconnect() {
        try {
            if(mSocket != null) {
                mSocket.close();
                //mSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "closeConnection() of connect socket failed", e);
        }
    }

    public void stop() {
        if(mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }

        if(mListenThread != null) {
            mListenThread.closeSocket();
            mListenThread = null;
        }

        mState = STATE_NONE;
    }

    @Override
    public void update(int action, Object obj) {
        mController.updateView(action, obj);
    }
}
