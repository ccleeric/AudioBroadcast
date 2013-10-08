package com.ccleeric.audiobroadcast;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by ccleeric on 13/9/30.
 */
public class AudioRecorder implements Notifier {
    private final String TAG = "AudioRecorder";

    public static final int AUDIO_SOURCE_UNSUPPORTED = 0x301;


    private final int AUDIO_RATE_HZ = 8000;                            //Sample Rate 44100,22050, 16000, and 11025Hz
    private final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;    // CHANNEL_IN_MONO, CHANNEL_IN_STEREO
    private final int AUDIO_FORMAT  = AudioFormat.ENCODING_PCM_16BIT;   //ENCODING_PCM_16BIT, ENCODING_PCM_8BIT
    private final String AUDIO_SOURCE_ERROR = "Audio Source doesn't support!";

    private BroadcastController mController;
    private AudioRecord mRecorder;
    private int mBufferSize;
    private byte[] mAudioData;
    private boolean mAudioStop;
    private Thread mRecorderThread;

    public AudioRecorder(BroadcastController controller) {
        mController = controller;
        mBufferSize = AudioRecord.getMinBufferSize(AUDIO_RATE_HZ,AUDIO_CHANNEL,AUDIO_FORMAT);
        mAudioData = new byte[mBufferSize];

        mAudioStop = true;
    }

    public void setRecordSource(String audioSource) {
        int source = MediaRecorder.AudioSource.MIC;

        if (audioSource.equals("Voice Call")) {
            source = MediaRecorder.AudioSource.VOICE_CALL;
        }

        if(mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        mRecorder = new AudioRecord(source, AUDIO_RATE_HZ,
                                AUDIO_CHANNEL, AUDIO_FORMAT, mBufferSize);
    }

    public void start(final OutputStream audioStream) {
        mAudioStop = false;

        if(mRecorderThread != null) {
            mRecorderThread = null;
        }

        mRecorderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes;
                try {
                    mRecorder.startRecording();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Audio Source doesn't support!", e);
                    update(AUDIO_SOURCE_UNSUPPORTED, AUDIO_SOURCE_ERROR);
                    mRecorder.release();
                    return;
                }

                while(!mAudioStop) {
                    bytes = mRecorder.read(mAudioData, 0, mBufferSize);

                    try {
                        audioStream.write(mAudioData, 0, mBufferSize);
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                        break;
                    }
                }
                mRecorder.stop();
                mRecorder.release();
            }
        });

        mRecorderThread.start();
    }

    public void stop() {
        mAudioStop = true;
    }

    @Override
    public void update(int action, Object obj) {
        mController.updateView(action, obj);
    }
}
