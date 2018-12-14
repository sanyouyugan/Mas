package com.qiudaoyu.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SecondActivity extends Activity {


    private int xxx = 0;

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
        a.test();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.btn)).setText("hello second ");
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SecondActivity.this, ThirdActivity.class));
            }
        });
    }

    protected int abc(int a, int b, int c) {
        return a + b + c;
    }

    protected int zzz(int a, int b, int c) {
        return a + b + c;
    }

    class A {
        private int x = xxx;

        private void test() {

        }

    }

}
