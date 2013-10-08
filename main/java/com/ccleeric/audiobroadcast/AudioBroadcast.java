package com.ccleeric.audiobroadcast;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class AudioBroadcast extends Activity {

    private final String TAG = "AudioBroadcast";

    public static final String TOAST = "toast";
    public static final String BLUETOOTH = "Bluetooth";
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_SETTINGS = 4;

    private BroadcastController mBroadcastController;
    private ListView mNewDevices;
    private ListView mPairedDevices;
    private TextView mTitlePairedDevices;
    private ListDevicesAdapter mNewDevicesAdapter;
    private ListDevicesAdapter mPairedDevicesAdapter;
    private ProgressBar mSearchProgress;

    private boolean mSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case AudioPlayer.ACTION_STOP:
                if (resultCode == Activity.RESULT_OK) {
                    mSender = false;
                    mBroadcastController.setHandler(mHandler);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    mBroadcastController.waitConnection();
                }
                break;
            case REQUEST_SETTINGS:
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mBroadcastController.getConnectionState() == BluetoothManager.STATE_NONE &&
                                         BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            mBroadcastController.waitConnection();
        }
    }

    public void init() {

        mNewDevices = (ListView) findViewById(R.id.new_devices);
        mNewDevicesAdapter = new ListDevicesAdapter(this, R.layout.list_item, new ArrayList<String>());
        mNewDevices.setAdapter(mNewDevicesAdapter);

        mNewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox box = (CheckBox) view.findViewById(R.id.checkbox);
                TextView address = (TextView) view.findViewById(R.id.mac_address);
                mSender = true;
                Intent intent = new Intent(AudioBroadcast.this, PlayerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("Sender", mSender);
                intent.putExtras(bundle);
                startActivityForResult(intent, AudioPlayer.ACTION_STOP);

                mBroadcastController.connectDevice((String) address.getText());
            }
        });

        mTitlePairedDevices = (TextView) findViewById(R.id.title_paired_devices);
        mPairedDevices = (ListView) findViewById(R.id.paired_devices);
        mPairedDevicesAdapter = new ListDevicesAdapter(this, R.layout.list_item, new ArrayList<String>());
        mPairedDevices.setAdapter(mPairedDevicesAdapter);


        mPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox box = (CheckBox) view.findViewById(R.id.checkbox);
                TextView address = (TextView) view.findViewById(R.id.mac_address);
                mSender = true;
                Intent intent = new Intent(AudioBroadcast.this, PlayerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("Sender", mSender);
                intent.putExtras(bundle);
                startActivityForResult(intent, AudioPlayer.ACTION_STOP);

                mBroadcastController.connectDevice((String) address.getText());
            }
        });

        showPairedDevices();

        mBroadcastController = BroadcastController.getInstance();
        mBroadcastController.setHandler(mHandler);
        mBroadcastController.setContext(this);


        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        mSearchProgress = (ProgressBar) findViewById(R.id.search_progress);
    }

    @Override
    protected void onDestroy() {
        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        setBeFoundItem(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_menu, menu);

        setBeFoundItem(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                mNewDevicesAdapter.clear();
                mNewDevicesAdapter.notifyDataSetChanged();

                mPairedDevicesAdapter.clear();
                mPairedDevicesAdapter.notifyDataSetChanged();

                showPairedDevices();

                mSearchProgress.setVisibility(View.VISIBLE);
                mBroadcastController.search();
                return true;
            case R.id.action_be_found:
                enableBluetoothBeFound();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    //set Bluetooth be found function in action bar
    public void setBeFoundItem(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_be_found);
        int action = MenuItem.SHOW_AS_ACTION_NEVER;
        if(isBluetoothOfConnectivity()) {
            action =MenuItem.SHOW_AS_ACTION_ALWAYS;
        }

        item.setShowAsAction(action);
    }

    /*
     * Enable bluetooth device to be found.
     */
    public void enableBluetoothBeFound() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
        startActivity(discoverableIntent);
    }

    public void showPairedDevices() {
        // Get a set of currently paired devices
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        mTitlePairedDevices.setVisibility(View.VISIBLE);
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesAdapter.add(device.getName() + "," + device.getAddress());
            }
        } else {
            mTitlePairedDevices.setVisibility(View.GONE);
        }
    }

    public boolean isBluetoothOfConnectivity() {
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        String connectivity = settingsPref.getString(SettingsActivity.PrefsFragement.CONNECTIVITY_PREF,"");

        boolean bluetooth = false;
        if(connectivity.equals(BLUETOOTH)) {
            bluetooth = true;
        }
        return bluetooth;
    }

    // Create a BroadcastReceiver for ACTION_FOUND & ACTION_DISCOVERY_FINISHED
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String title = device.getName() + "," + device.getAddress();

                // Check the found device is exist in paired devices or new devices
                if (device.getBondState() != BluetoothDevice.BOND_BONDED &&
                                        mNewDevicesAdapter.getPosition(title) < 0)  {
                    // Add the name and address to an array adapter to show in a ListView
                    mNewDevicesAdapter.add(title);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mSearchProgress.setVisibility(View.GONE);
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case AudioPlayer.ACTION_PLAY:
                    mSearchProgress.setVisibility(View.GONE);
                    Intent intent = new Intent(AudioBroadcast.this, PlayerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("Sender", mSender);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, AudioPlayer.ACTION_STOP);
                    break;
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }
}
