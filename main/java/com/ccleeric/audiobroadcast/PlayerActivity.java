package com.ccleeric.audiobroadcast;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Created by ccleeric on 13/10/4.
 */
public class PlayerActivity extends Activity {
    private final String TAG = "PlayerActivity";
    public static final String TOAST = "toast";

    private BroadcastController mController;
    private ImageButton mAudioStop;
    private ProgressBar mWaitConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        boolean sender = bundle.getBoolean("Sender");
        mController = BroadcastController.getInstance();
        mController.setHandler(mHandler);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.player);

        mAudioStop = (ImageButton) findViewById(R.id.audio_stop);
        mAudioStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.stopAudio();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        mWaitConnection = (ProgressBar) findViewById(R.id.wait_connection);
        if(sender) {
            mAudioStop.setVisibility(View.GONE);
            mWaitConnection.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK ||
                        keyCode == KeyEvent.KEYCODE_HOME) {
            mController.closeConnection();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case AudioPlayer.ACTION_PLAY:
                    mAudioStop.setVisibility(View.VISIBLE);
                    mWaitConnection.setVisibility(View.GONE);
                    break;
                case AudioRecorder.AUDIO_SOURCE_UNSUPPORTED:
                case AudioPlayer.ACTION_STOP:
                case BluetoothManager.ACTION_CONNECT_LOST:
                case BluetoothManager.ACTION_CONNECT_FAILED:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();

                    setResult(Activity.RESULT_OK);
                    finish();
                    break;
            }
        }
    };
}
