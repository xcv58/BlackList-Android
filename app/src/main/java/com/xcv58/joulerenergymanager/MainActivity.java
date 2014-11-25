package com.xcv58.joulerenergymanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xcv58 on 11/24/14.
 */
public class MainActivity extends Activity {
    public final static String TAG = "JoulerEnergyManageActivity";
    public final static String DEFAULT = "Default";
    public final static String BLACK_LIST = "BlackList";
    public final static String WHITE_LIST = "WhiteList";
    public final static String LIFE_TIME = "LifeTime";
    private final static String[] POLICY_OPTIONS = new String[] {DEFAULT, BLACK_LIST, WHITE_LIST, LIFE_TIME};
    private OptionsAdapter optionsAdapter;
    private List<MyOption> optionList;
    private Button button;
    private JoulerEnergyManageDeamon mService;
    private boolean mBound;
    private Intent joulerenergyManageDeamonIntent;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d(TAG, "ServiceConnection");
            JoulerEnergyManageDeamon.LocalBinder binder = (JoulerEnergyManageDeamon.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            // update mService and optionList
            Log.d(TAG, "Get choice: " + mService.getChoice());
            changeButton(mService.getChoice());
            for (MyOption option : optionList) {
                option.setService(mService);
            }
            optionsAdapter.notifyDataSetChanged();
            Log.d(TAG, "bind service successful");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        joulerenergyManageDeamonIntent = new Intent(this, JoulerEnergyManageDeamon.class);
        startService(joulerenergyManageDeamonIntent);
        bindService(joulerenergyManageDeamonIntent, mConnection, this.BIND_AUTO_CREATE);

        setContentView(R.layout.main_activity);

        final ListView listView = (ListView) findViewById(R.id.policy_options);
        optionList = new ArrayList<MyOption>();
        for (String str : POLICY_OPTIONS) {
            optionList.add(new MyOption(str));
        }
        optionsAdapter = new OptionsAdapter(this, optionList);
        listView.setAdapter(optionsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Log.d(TAG, "CLICK " + POLICY_OPTIONS[position]);
                mService.putChoice(POLICY_OPTIONS[position]);
                changeButton(POLICY_OPTIONS[position]);
                optionsAdapter.notifyDataSetChanged();
            }
        });

        button = (Button) findViewById(R.id.button);
    }

    private void changeButton(String option) {
        // call service to change policy
        button.setText(option);
//        button.setClickable(!option.equals(DEFAULT));
//        button.setVisibility((option.equals(DEFAULT)) ? View.INVISIBLE : View.VISIBLE);
    }

    public void enterSetting(View v) {
        button = (Button) v;
        Log.d(TAG, "click" + button.getText());
        String option = (String) button.getText();
        Intent intent = null;
        if (option.equals(DEFAULT)) {
        } else if (option.equals(BLACK_LIST)) {
            intent = new Intent(this, BlackListActivity.class);
        } else if (option.equals(WHITE_LIST)) {
        } else if (option.equals(LIFE_TIME)) {

        } else {
            Log.e(TAG, option + " is impossible button");
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
