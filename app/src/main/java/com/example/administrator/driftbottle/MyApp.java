package com.example.administrator.driftbottle;

import android.app.Application;
import android.content.Intent;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.example.administrator.driftbottle.utils.TTAdManagerHolder;

import java.util.logging.Logger;

public class MyApp extends Application {
    private static MyApp instance;
    public boolean isOpenSplash = false;
    @Override
    public void onCreate () {
        super.onCreate();
        instance = this;

        TTAdManagerHolder.init(this);

//        startActivity(new Intent(this, SplashActivity.class));
    }
    public static MyApp getInstance() {
        return instance;
    }
}
