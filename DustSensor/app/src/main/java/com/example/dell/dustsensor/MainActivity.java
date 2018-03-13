package com.example.dell.dustsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final String deviceAddress = "00:11:35:93:37:94";
    private boolean isConnected = false;
    private Button bluetoothButton,connectButton;
    private RatingBar ratingBar;
    private TextView displayDust, displayHum, displayTemp, qualityView,pmView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;


    public void enableBluetooth()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            System.out.println("Your device does not support bluetooth");
        }

        if(mBluetoothAdapter.isEnabled()){
            bluetoothButton.setText("Switch on bluetooth");
            mBluetoothAdapter.disable();
            isConnected = false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            bluetoothButton.setText("Disable bluetooth");
            mBluetoothAdapter.enable();
            isConnected = true;


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothButton = (Button) findViewById(R.id.button);
        connectButton = (Button) findViewById(R.id.button2);
        displayDust = (TextView) findViewById(R.id.textView4);
        pmView = (TextView) findViewById(R.id.textView2);
        displayTemp = (TextView) findViewById(R.id.textView3);
        displayHum = (TextView) findViewById(R.id.textView6);
        qualityView = (TextView) findViewById(R.id.textView5);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        ratingBar.setIsIndicator(true);
        ratingBar.setClickable(false);
        ratingBar.setRating(0);
        qualityView.setText("");
        pmView.setText("");
        displayHum.setText("Humidity");
        displayTemp.setText("Temperature");

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected){
                try {
                    mDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                } catch (IllegalArgumentException e) {  }
                mConnectThread = new ConnectThread(mDevice);
                mConnectThread.start();
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_LONG).show();
                }
            }});
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException e) {   }
                return;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private BufferedReader mmInStream;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            BufferedReader tmpIn = null;
            try {
                tmpIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
            }
            mmInStream = tmpIn;

        }


        public void run() {
            String results;
            int dust = 0, temp = 0, hum = 0;

            while (mmInStream != null) {

                try {
                    results = mmInStream.readLine();

                   if(results.length() >1) {
                       String[] tableOfMeasuredValues = results.split("[vxz]");

                       dust = Integer.parseInt(tableOfMeasuredValues[1].substring(1));
                       temp = Integer.parseInt(tableOfMeasuredValues[2]);
                       hum = Integer.parseInt(tableOfMeasuredValues[3]);

                       mHandler.obtainMessage(dust, temp, hum).sendToTarget();

                   }

                } catch (IOException e) {
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void ratingBar(int number)
    {
        if(number < 35)
        {
            ratingBar.setRating(6);
            qualityView.setText("Very good!");
        }else if((number <75)&(number>=35))
        {
            ratingBar.setRating(5);
            qualityView.setText("Good");
        }else if((number <115)&(number>=75))
        {
            ratingBar.setRating(4);
            qualityView.setText("Light pollution");
        }else if((number <150)&(number>=115))
        {
            ratingBar.setRating(3);
            qualityView.setText("Moderate pollution");
        }else if((number <250)&(number>=150))
        {
            ratingBar.setStepSize(2);
            qualityView.setText("Bad");
        }else if(number>=250)
        {
            ratingBar.setRating(1);
            qualityView.setText("Better stay at home!");
        }else{
            qualityView.setText("");
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int dust = msg.what;
            int temp = msg.arg1;
            int hum = msg.arg2;
            qualityView.setText("");
            ratingBar(dust);
            displayDust.setText(String.valueOf(dust));
            pmView.setText("μg/m3");
            displayTemp.setText(String.valueOf(temp) + "°C");
            displayHum.setText(String.valueOf(hum) + " rh");
            }
    };



}
