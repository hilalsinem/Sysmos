package com.ieu.ex_9_read_from_iot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothManager btManager = null;
    private BluetoothAdapter btAdapter = null;
    private BluetoothLeScanner bleScanner = null;
    private BluetoothDevice btDevice = null;
    private BluetoothGatt btGatt = null;
    private final int REQUEST_ENABLE_BLUETOOTH = 1;
    private String contents = "";
    private BluetoothGattCharacteristic batteryLevelChar = null;
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("00001803-0000-1000-8000-00805F9B34FB");

    //----------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocation();

        setClickHandlerForStartScan();
        setClickHandlerForStopScan();
        setClickHandlerForConnect();
        setClickHandlerForDisconnect();
    }
    //----------------------------------------------------------------------------------------------
    private void checkLocation() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("MainActivity", "Location is already ON");
            checkBLE();
        }
        else {
            Log.d("MainActivity", "Turn on Location !!!");
            Toast.makeText(MainActivity.this, "EXIT, ENABLE LOCATION AND RE-RUN THIS APP!", Toast.LENGTH_LONG).show();
        }
    }
    //----------------------------------------------------------------------------------------------
    private void checkBLE() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // BLE is supported. Continue.
            checkPermissions();
        } else {
            // BLE is not supported. Display a message and stop.
            Toast.makeText(MainActivity.this, "BLE is not supported, exiting...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    //----------------------------------------------------------------------------------------------
    private void checkPermissions() {
        Log.d("MAIN", "ACCESS_FINE_LOCATION: " + ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));

        Log.d("MAIN", "Android version: " + android.os.Build.VERSION.SDK_INT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S){
            Log.d("MAIN", "Android version greater than S");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            Log.d("MAIN", "Android version less than S");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
        }
    }
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("MAIN", "onRequestPermissionsResult() --> requestCode: " + requestCode);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MAIN", "onRequestPermissionsResult() --> permission granted for " + requestCode);
                    checkBT();
                } else {
                    Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MAIN", "onRequestPermissionsResult() --> permission granted for " + requestCode);
                    checkBT();
                } else {
                    Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.e("MAIN", "onRequestPermissionsResult() --> wrong requestCode: " + requestCode);
                break;
        }
    }
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void checkBT() {
        btManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        Button btn = (Button) findViewById(R.id.btnStartScan);
        btn.setEnabled(true);

        if (btAdapter.isEnabled()) {
            Log.d("MAIN", "BT is enabled");
        } else {
            Log.d("MAIN", "BT is NOT enabled");

            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.S){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                btAdapter.enable();
                Toast.makeText(MainActivity.this, "BT enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MAIN", "onActivityResult: bt request " + (resultCode == Activity.RESULT_OK ? "granted" : "rejected"));

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(MainActivity.this, "BT enabled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "BT not enabled, exiting.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void setClickHandlerForStartScan() {
        Button btnStartScan = (Button) findViewById(R.id.btnStartScan);
        btnStartScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bleScanner = btAdapter.getBluetoothLeScanner();
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build();
                final List<ScanFilter> filters = new ArrayList<>();
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(new UUID(0xEF6801009B354933L, 0x9B1052FFA9740042L))).build());
                //bleScanner.startScan(filters, settings, mScanCallback);
                bleScanner.startScan(null, settings, mScanCallback);
                Toast.makeText(MainActivity.this, "Scan started", Toast.LENGTH_SHORT).show();
                Button btn = (Button) findViewById(R.id.btnStopScan);
                btn.setEnabled(true);
            }
        });
    }
    //----------------------------------------------------------------------------------------------
    private ScanCallback mScanCallback = new ScanCallback() {
        private Handler mCallbackHandler = new Handler(Looper.getMainLooper());
        //------------------------------------------------------------------------------------------
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.d("MAIN", "onScanResult. Device: " + result.getDevice() + ", name: " + result.getScanRecord().getDeviceName());

            List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();

            if (uuids != null) {
                //Log.d("MAIN", "uuid len: " + uuids.size());

                for (ParcelUuid uuid : uuids) {
                    //Log.d("MAIN", "uuid: " + uuid.toString());

                    if (uuid.equals(SERVICE_UUID)) {
                        Log.d("MAIN", "correct uuid found: " + uuid.toString());

                        int instantRSSI = result.getRssi();
                        Log.d("MAIN", "instant rssi: " + result.getRssi());

                        int rssiAt1Mt = -75;
                        double N = 2.4;

                        double distance = Math.pow(10, ((rssiAt1Mt - instantRSSI)/(10 * N)));
                        Log.d("MAIN", "distance: " + distance);

                        TextView txtDevice = (TextView) findViewById(R.id.txtDevice);
                        txtDevice.setText(result.getDevice().getAddress() + ": " + distance + " m");

                        btDevice = result.getDevice();
                        Button btnConnect = (Button) findViewById(R.id.btnConnect);
                        btnConnect.setEnabled(true);

//                        final byte[] data = result.getScanRecord().getBytes();
//
//                        if (data == null) {
//                            Log.w("LocalService", "Invalid scan result, do nothing. Device: " + result.getDevice() + ", name: " + result.getDevice().getName());
//                        } else {
//                            Log.d("LocalService", "Data found. Device Address: " + result.getDevice().getAddress() + ", friendly name: " + result.getDevice().getName() + ", data_len: " + data.length);
//
//                            String nutData = "";
//
//                            for (int i = 0; i < data.length; i++) {
//                                nutData = nutData.concat("0x" + String.format("%02X", data[i]) + " ");
//                            }
//                            Log.d("LocalService", " data: " + nutData);
//
//                            if (result.getDevice().getAddress().equals("D5:8A:2E:54:AD:96")) {
//                                Log.d("LocalService", "ALERT ALERT ALERT !!!");
//                            }
//                        }
//                    }
//                            }
                        //break;
                    }
                }

            }


        }
        //------------------------------------------------------------------------------------------
        @Override
        public void onScanFailed(int errorCode) {
            Log.w("MAIN", "Scan Error Code: " + errorCode);
        }
        //------------------------------------------------------------------------------------------
        @SuppressLint("MissingPermission")
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d("MAIN", "onBatchScanResults: " + results);

//            for (ScanResult result : results) {
            //Log.d("MAIN", "address: " + result.getDevice().getAddress());

//                if (result.getDevice().getAddress().equalsIgnoreCase("d6:02:ec:da:58:be")) {
//                    btDevice = result.getDevice();
//                    TextView txtDevice = (TextView) findViewById(R.id.txtDevice);
//                    txtDevice.setText("Nordic Thingy (" + btDevice.getAddress() + ")");
//                    Button btnConnect = (Button) findViewById(R.id.btnConnect);
//                    btnConnect.setEnabled(true);
//                }
//            }
        }
    };
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void setClickHandlerForStopScan() {
        Button btnStopScan = (Button) findViewById(R.id.btnStopScan);
        btnStopScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bleScanner.stopScan(mScanCallback);
                Toast.makeText(MainActivity.this, "Scan stopped", Toast.LENGTH_SHORT).show();
                btnStopScan.setEnabled(false);

            }
        });
    }
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void setClickHandlerForConnect() {
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btGatt = btDevice.connectGatt(getApplicationContext(), false, btGattCallback);
                Toast.makeText(MainActivity.this, "connect called", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //----------------------------------------------------------------------------------------------
    private BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        //------------------------------------------------------------------------------------------
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            super.onConnectionStateChange(gatt, status, newState);

            Log.d("MAIN", "onConnectionStateChange() status: " + status + ", newState: " + newState);
            //btGatt.discoverServices();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("MAIN", "onConnectionStateChange GATT_SUCCESS");

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d("MAIN", "onConnectionStateChange STATE_CONNECTED");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
                            btnDisconnect.setEnabled(true);
                        }
                    });

                    btGatt.discoverServices();

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("MAIN", "onConnectionStateChange STATE_DISCONNECTED");
                    btGatt.close();
                }
                //} else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                //    Log.d("MAIN", "onConnectionStateChange STATE_DISCONNECTED");
                //    btGatt.close();
            }
        }

        //------------------------------------------------------------------------------------------
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("MAIN", "onServicesDiscovered GATT_SUCCESS");

                BluetoothGattService batteryService = gatt.getService(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")); // BATTERY_SERVICE
                batteryLevelChar = batteryService.getCharacteristic(UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")); // BATTERY_SERVICE_CHARACTERISTIC"
                //gatt.readCharacteristic(batteryLevelChar);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gatt.readCharacteristic(batteryLevelChar);
                    }
                }, 1000);
            }
        }
        //------------------------------------------------------------------------------------------
        @Override
        public final void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (characteristic.equals(batteryLevelChar)) {
                int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                Log.d("MAIN", "batteryLevel : " + batteryLevel);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView txtBattery = (TextView) findViewById(R.id.txtBattery);
                        txtBattery.setText("Battery Level: " + batteryLevel);
                    }
                });
            }
        }
    };
    //----------------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void setClickHandlerForDisconnect() {
        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btGatt.disconnect();
                Toast.makeText(MainActivity.this, "disconnect called", Toast.LENGTH_SHORT).show();
                btnDisconnect.setEnabled(false);
            }
        });
    }
    //----------------------------------------------------------------------------------------------
}