package com.xcv58.joulerenergymanager;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LifetimeManagerActivity extends Activity {

    final static String TAG = "LifetimeManagerActivity";
    Button bt;
    EditText softThreshold = null;
    EditText criticalThreshold = null;
    EditText lifetime = null;
    TextView description;
    final String softDescription = "This is a soft threshold that indicates the battery level at which the energy manager will"
            + " start to monitor and manage energy consumption of your device. Ideally it should be between 80 and 40";
    final String criticalDescription = "This is a critical threshold that indicates the battery level at which the energy manager"
            + " will aggressivly try to control energy consumption to reach the expected lifetime goals. Ideally it should be "
            + "between 30 and 10";
    final String lifetimeDescription = "This is the maximum number of hours you want your device battery to last in one discharging "
            + "cycle or in other words maximum number of hours between to full charging events when your device is charged to full 100%";

    static boolean service = false;
    private boolean mBound = false;
    private LifetimeManagerService mService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Log.d(TAG, "ServiceConnection");
            LifetimeManagerService.LocalBinder binder = (LifetimeManagerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //Log.d(TAG, "bind service successful");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;

        }

    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lifetime);
        bt = (Button) findViewById(R.id.button1);
        softThreshold = (EditText) findViewById(R.id.threshold1);
        criticalThreshold = (EditText) findViewById(R.id.threshold2);
        lifetime = (EditText) findViewById(R.id.threshold3);
        description = (TextView) findViewById(R.id.Description4);


        softThreshold.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                description.setText(softDescription);
                return false;
            }
        });

        criticalThreshold.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                description.setText(criticalDescription);
                return false;
            }
        });

        lifetime.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                description.setText(lifetimeDescription);
                return false;
            }
        });

        bt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                done();

            }
        });

    }

    public void done() {
        if(softThreshold.getText().toString().equals("") || criticalThreshold.getText().toString().equals("")
                || this.lifetime.getText().toString().equals("")){
            description.setText("Enter 3 values: soft threshold, critical threshold & lifetime in hours");
            return;
        }
        int threshold1 = Integer.parseInt( softThreshold.getText().toString());
        int threshold2 = Integer.parseInt(criticalThreshold.getText().toString());
        int lifetime = Integer.parseInt(this.lifetime.getText().toString());
        if (threshold1 < 0 || threshold1 > 100 || threshold1 <= threshold2) {
            description.setText("Soft Threshold value is set incorrectly. Either it is not within the range 0 to 100"
                    + " or it is < = to the value of critical threshold");
            return;
        }
        if(threshold2 < 0 || threshold2 > 100) {
            description.setText("Critical Threshold value is set incorrectly. It should lie within the range from 0 to 100");
            return;
        }
        if(lifetime < 1 || lifetime > 24) {
            description.setText("Lifetime hours are set incorrectly. It should be a value within 1 (hour) to 24 (hours)");
            return ;
        }

        mService.setParams("soft", threshold1);
        mService.setParams("critical", threshold2);
        mService.setParams("lifetime", lifetime);
        mService.flush();
        startService(new Intent(this, LifetimeManagerService.class));
        //finish();
        bt.setBackgroundColor(Color.TRANSPARENT);
        bt.setClickable(false);

    }
    @Override
    protected void onResume() {
        super.onResume();
        bt.setBackgroundColor(Color.GRAY);
        bt.setClickable(true);
        bindService(new Intent(this,LifetimeManagerService.class),mConnection,this.BIND_AUTO_CREATE);
    }


    @Override
    protected void onPause() {
//        Log.d(TAG, "on Pause");
        if(mService !=null)
            mService.flush();
        super.onPause();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

    }

    @Override
    protected void onStop() {
        //Log.d(TAG, "on Stop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.flush();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if(!softThreshold.getText().toString().equals(""))
            savedInstanceState.putInt("softThreshold", Integer.parseInt(softThreshold.getText().toString()));
        if(!criticalThreshold.getText().toString().equals(""))
            savedInstanceState.putInt("criticalThreshold", Integer.parseInt(criticalThreshold.getText().toString()));
        if(!lifetime.getText().toString().equals(""))
            savedInstanceState.putInt("lifetime", Integer.parseInt(lifetime.getText().toString()));
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.containsKey("softThreshold"))
            softThreshold.setText(savedInstanceState.getInt("softThreshold"));
        if(savedInstanceState.containsKey("criticalThreshold"))
            criticalThreshold.setText(savedInstanceState.getInt("criticalThreshold"));
        if(savedInstanceState.containsKey("lifetime"))
            lifetime.setText(savedInstanceState.getInt("lifetime"));
    }
}
