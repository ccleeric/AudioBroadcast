package com.ccleeric.audiobroadcast;

import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

/**
 * Created by ccleeric on 13/9/25.
 */
public class BroadcastController {

    private final String TAG = "AudioBroadcastController";

    private volatile static BroadcastController mInstance;

    private BluetoothManager mBtManager;
    private AudioRecorder mAudioRecorder;
    private AudioPlayer mAudioPlayer;
    private Context mContext;
    private Handler mHandler;
    private boolean mSender;

    private BroadcastController() {
        mBtManager = new BluetoothManager(this);
        mAudioRecorder = new AudioRecorder(this);
        mAudioPlayer = new AudioPlayer(this);
        mSender = false;
    }

    public static synchronized BroadcastController getInstance() {
        if(mInstance == null) {
            mInstance = new BroadcastController();
        }
        return mInstance;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public BluetoothManager getBtManager() {
        return mBtManager;
    }

    public void search() {
        mBtManager.doDiscovery();
    }

    public int getConnectionState() {
        return mBtManager.getState();
    }

    public void waitConnection() {
        mBtManager.listen();
    }

    public void connectDevice(String address) {
        mSender = true;
        mBtManager.connect(address);
    }

    public void stopAudio() {
        if(mSender) {
            mAudioRecorder.stop();
        }

        mBtManager.disconnect();
    }

    public void playAudio() {
        mSender = false;

        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String audioSource = settingsPref.getString(SettingsActivity.PrefsFragement.AUDIO_SOURCE_PREF,"");
        mAudioRecorder.setRecordSource(audioSource);

        mAudioRecorder.start(mBtManager.getAudioOutStream());
    }

    public void closeConnection() {
        mBtManager.stop();
        mBtManager.disconnect();
    }

    public void sendMessage(int action, String text) {
        Message msg = mHandler.obtainMessage(action);
        Bundle bundle = new Bundle();
        bundle.putString(AudioBroadcast.TOAST, text);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public void updateView(int action, Object obj) {
        switch(action) {
            case AudioPlayer.ACTION_PLAY:
                mAudioPlayer.play((InputStream) obj);
                sendMessage(action, null);
                if(mSender) {
                    playAudio();
                }
                break;
            case AudioRecorder.AUDIO_SOURCE_UNSUPPORTED:
                sendMessage(action, (String)obj);
                mBtManager.disconnect();
                break;
            case BluetoothManager.ACTION_CONNECT_FAILED:
                sendMessage(action, (String)obj);
                break;
            case BluetoothManager.ACTION_CONNECT_LOST:
                mBtManager.connectLost();
                sendMessage(action, (String)obj);
                break;
            default:
                break;
        }
    }
}
