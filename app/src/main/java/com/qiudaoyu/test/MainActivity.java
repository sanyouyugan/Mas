package com.qiudaoyu.test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.qiudaoyu.service.DataParcelable;
import com.qiudaoyu.service.LocalService;
import com.qiudaoyu.service.RemoteService;
import com.qiudaoyu.service.DataParcelable;
import com.qiudaoyu.service.LocalService;
import com.qiudaoyu.service.RemoteService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class MainActivity extends Activity {


    private int xxx = 0;
    private LocalService mService;
    private boolean mBound;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void main(String[] args) {
        JSONObject object = new JSONObject();
        try {
            object.put("applyEntityIdList", new ArrayList<String>());
            System.out.print(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        A a = new A();
        a.x = 0;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
        findViewById(R.id.startlocal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        findViewById(R.id.startremote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoteService.class);
                intent.putExtra("abc", new DataParcelable());
                startService(intent);

            }
        });

        findViewById(R.id.startlocal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoteService.class);
                intent.putExtra("abc", new DataParcelable());
                bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {


                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {


                    }
                }, BIND_AUTO_CREATE);


            }
        });


        int ii = 0;
        switch (ii) {
            case 0:
                return;
            case 1:
                return;
            case 2:
                return;
            case 3:
                return;
        }

    }

    protected int abc(int a, int b, int c) {
        return a + b + c;
    }

    protected int zzz(int a, int b, int c) {
        return a + b + c;
    }

    class A {
        private int x = xxx;

        private void test(String a) {

        }

        private void test2(String a) {
            try {
                test(a);
            } catch (Exception e) {
                test(a);
            } finally {
                test(a);
            }
        }

    }

    class aa extends AbstractQueuedSynchronizer {

    }
}
