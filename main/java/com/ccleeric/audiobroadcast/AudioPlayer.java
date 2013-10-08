package com.ccleeric.audiobroadcast;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ccleeric on 13/9/30.
 */
public class AudioPlayer implements Notifier {
    private final String TAG = "AudioPlayer";

    public static final int ACTION_PLAY  = 0x201;
    public static final int ACTION_STOP  = 0x202;


    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;          //
    private final int AUDIO_RATE_HZ = 8000;                            //Sample Rate 44100,22050, 16000, and 11025Hz
    private final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;    // CHANNEL_IN_MONO, CHANNEL_IN_STEREO
    private final int AUDIO_FORMAT  = AudioFormat.ENCODING_PCM_16BIT;   //ENCODING_PCM_16BIT, ENCODING_PCM_8BIT
    private final int AUDIO_TRACK_MODE = AudioTrack.MODE_STREAM;
    private final String DEVICE_LOST_MESSAGE = "Device connection was lost";

    private BroadcastController mController;
    private AudioTrack mAudioTrack;
    private Thread mPlayThread;
    private int mBufferSize;
    private byte[] mAudioData;
    private boolean mAudioStop;

    public AudioPlayer(BroadcastController controller) {
        mController = controller;
        mBufferSize = AudioTrack.getMinBufferSize(AUDIO_RATE_HZ, AUDIO_CHANNEL, AUDIO_FORMAT);
        mAudioTrack = new AudioTrack(STREAM_TYPE, AUDIO_RATE_HZ, AUDIO_CHANNEL,
                                            AUDIO_FORMAT, mBufferSize, AUDIO_TRACK_MODE);

        mAudioData = new byte[mBufferSize];
        mAudioStop = true;
    }

    public void play(final InputStream audioStream) {
        mAudioStop = false;

        if(mPlayThread != null) {
            mPlayThread = null;
        }

        mPlayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes;

                Log.d(TAG, "Begin read audio stream!");
                mAudioTrack.play();
                while(!mAudioStop) {
                    try {
                        //Log.i(TAG, "Stream playAudio playing");
                        bytes = audioStream.read(mAudioData);

                        mAudioTrack.write(mAudioData, 0, mAudioData.length);
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                        Log.d(TAG, "audio player exception!");
                        update(BluetoothManager.ACTION_CONNECT_LOST, DEVICE_LOST_MESSAGE);
                        break;
                    }
                }
                mAudioTrack.stop();
                Log.d(TAG, "audio player stopAudio");
            }
        });

        mPlayThread.start();
    }

    public void stop() {
        mAudioStop = true;
        mPlayThread = null;
    }

    @Override
    public void update(int action, Object obj) {
        mController.updateView(action, obj);
    }
}
