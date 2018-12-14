package com.qiudaoyu.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.qiudaoyu.monitor.df.MMonitor;
import com.qiudaoyu.monitor.log.mlog.annotation.MAop;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.schedulers.Schedulers;

public class ThirdActivity extends Activity {


//    private static int xxx1 = 0;
//    private int xxx = 0;

    @MAop(params = {"1", "2", "3"})
    public static Integer cdze() {
        int b = 5;
        int c = 6;
        return null;
    }

    @MAop(params = {"1", "2", "3"})
    public int ccc() {
        int b = 5;
        int c = 6;
        return b;
    }
//
//    public void main(String[] args) {
//        JSONObject object = new JSONObject();
//        try {
//            object.put("applyEntityIdList", new ArrayList<String>());
//            System.out.print(object.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        A a = new A();
//        a.x = 0;
//        a.test();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        findViewById(R.id.anr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JSONObject object = new JSONObject();
                try {
                    object.put("data", "1");
                    object.put("data1", "2");
                    object.put("data2", "3");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MMonitor.getInstance().trackBusUserEvent("clickAnr", object);

                MMonitor.getInstance().setParentId(null);

                synchronized (ThirdActivity.this) {
                    try {
                        ThirdActivity.this.wait(20000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });

        findViewById(R.id.gc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject object = new JSONObject();

                try {
                    object.put("data", "1");
                    object.put("data1", "2");
                    object.put("data2", "3");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MMonitor.getInstance().trackBusUserEvent("clickGc", object);

                MMonitor.getInstance().setParentId(null);
                for (int i = 0; i < 50; i++) {
                    new String("ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" +
                            "sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" +
                            "ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
                }
                System.gc();
            }
        });

        findViewById(R.id.http).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JSONObject object = new JSONObject();
                try {
                    object.put("data", "1");
                    object.put("data1", "2");
                    object.put("data2", "3");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MMonitor.getInstance().trackBusUserEvent("clickHttp", object);

                final String pd = MMonitor.getInstance().getParentId();


                RetrofitService.getProxy(LoginApi.class)
                        .doLogin("", "")
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Subscriber<HttpResult>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                JSONObject object = new JSONObject();
                                try {
                                    object.put("data", "1");
                                    object.put("data1", "2");
                                    object.put("data2", "3");
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                MMonitor.getInstance().trackBusUserEvent("afterclickHttp", object, pd);

                            }

                            @Override
                            public void onNext(HttpResult httpResult) {
                                JSONObject object = new JSONObject();
                                try {
                                    object.put("data", "1");
                                    object.put("data1", "2");
                                    object.put("data2", "3");
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                MMonitor.getInstance().trackBusUserEvent("afterclickHttp", object, pd);

                            }
                        });
            }
        });
        findViewById(R.id.crash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("monitor crash");
            }
        });

    }

//    @MAop(params = {"aaa", "1", "2", "3"})
//    protected Activity aaa(int a, int b, int c) {
//        int x = 0;
//        int z = 1;
//        return this;
//    }
//
//    @MAop(params = {"newLocal", "1", "2", "3"})
//    protected int newLocal(int a, int b, int c) {
//        int x = 0;
//        int z = 1;
//
//        return a + b + c + x + z;
//    }
//
//    protected int abc(int a, int b, int c) {
//        return a + b + c;
//    }
//
//    @MAop(params = {"zzz", "1", "2", "3"})
//    protected int zzz(int a, int b, int c) {
//        int z = 1;
//        return a + b + c + z;
//    }
//
//    @MAop(params = {"xxx", "1", "2", "3"})
//    protected int xxx() {
//        int b = 1;
//        return b;
//    }
//
//    @MAop(params = {"activity", "1", "2", "3"})
//    protected int activity(Activity activity) {
//        int b = 1;
//        return b;
//    }
//
//    void test() {
//
//    }
//
//    class A {
//        private int x = xxx;
//
//        private void test() {
//
//        }
//    }

    public static final class Builder {
        public ThirdActivity.Builder connectTimeout(long timeout, TimeUnit unit) {
            return this;
        }

        public ThirdActivity.Builder xxxxxx(long timeout, TimeUnit unit) {
            return this;
        }
    }

}
