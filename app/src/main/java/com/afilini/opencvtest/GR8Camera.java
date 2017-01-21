package com.afilini.opencvtest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.afilini.opencvtest.ev3client.EV3ClientConnection;
import com.afilini.opencvtest.joypadserver.ConnectionListener;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class GR8Camera extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private TextView info;
    private TextView current_IP;

    private EditText EV3_Input;
    private CheckBox save_IP;
    private Button EV3_Connect;

    private SharedPreferences preferences;
    private CameraBridgeViewBase mOpenCvCameraView;

    private final ConnectionListener phoneConnection;
    private EV3ClientConnection EV3Connection;


    public GR8Camera() {
        phoneConnection = new ConnectionListener(8080, this, this.info);
        phoneConnection.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        info = (TextView) findViewById(R.id.info);
        current_IP = (TextView) findViewById(R.id.current_IP);

        info.setMovementMethod(new ScrollingMovementMethod());

        EV3_Input = (EditText) findViewById(R.id.EV3_Input);
        save_IP = (CheckBox) findViewById(R.id.EV3_Check);
        EV3_Connect = (Button) findViewById(R.id.EV3_Connect);

        preferences = getPreferences(MODE_PRIVATE);


        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String networkName = info.getSSID();
        int baseIP = info.getIpAddress();
        String ipString = String.format("%d.%d.%d.%d", (baseIP & 0xff), (baseIP >> 8 & 0xff), (baseIP >> 16 & 0xff), (baseIP >> 24 & 0xff));

        if (ipString.equals("0.0.0.0")) {
            current_IP.setText("Currently not in a WiFi network.");
        } else {
            current_IP.setText("Using address " + ipString + " on network " + networkName + ".");
        }


        String savedIP = preferences.getString("IP", null);
        if (savedIP != null) {
            EV3_Input.setText(savedIP);
        }

        boolean checkedState = preferences.getBoolean("isChecked", false);
        if (checkedState) {
            save_IP.setChecked(true);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        save_IP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            SharedPreferences.Editor edit = preferences.edit();

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {
                    edit.putString("IP", EV3_Input.getText().toString()).apply();
                    edit.putBoolean("isChecked", true).apply();
                } else {
                    edit.remove("IP").apply();
                    edit.remove("isChecked").apply();
                }
            }
        });

        EV3_Connect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                info.append("Trying to connect...\n");
                try {
                    EV3Connection = new EV3ClientConnection(InetAddress.getByName(EV3_Input.getText().toString()), 2305);
                    EV3Connection.start();

                    Runnable readCommand = new Runnable() {
                        @Override
                        public void run() {
                            phoneConnection.readCommand(EV3Connection);
                        }
                    };

                    Thread readCommandThread = new Thread(readCommand);
                    readCommandThread.start();
                    info.append("Connection established with " + EV3Connection.getIpAddress() + ".\n");
                } catch (UnknownHostException exc) {
                    info.append("Unknown host.\n");
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;

                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

        try {
            final Bitmap image = phoneConnection.sendImage(mRgbaT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mRgbaT;
    }

    public void editInfo(final String text) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                info.append(text + "\n");
            }
        });
    }
}
