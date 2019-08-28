package com.example.administrator.driftbottle;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.example.administrator.driftbottle.utils.TTAdManagerHolder;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelpay.JumpToOfflinePay;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.open.SocialConstants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.administrator.driftbottle.wxapi.NetworkUtil;
import com.zlw.main.recorderlib.RecordManager;
import com.zlw.main.recorderlib.recorder.RecordConfig;
import com.zlw.main.recorderlib.recorder.RecordHelper;
import com.zlw.main.recorderlib.recorder.listener.RecordResultListener;
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener;

public class MainActivity extends AppCompatActivity {

    private WebView webview;
    private static Tencent mTencent;
    private UserInfo mInfo;

    public static final String RSA2_PRIVATE = "";
    public static final String RSA_PRIVATE = "";

    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;

    private static boolean isServerSideLogin = false;

    private IWXAPI api;
    private static final String APP_ID = "wx8696982e0c9df979";
    private String user_openId, accessToken, refreshToken, scope;
    private LocationManager locationManager;
    private String locationProvider;
    private static AudioRecorder audioRecorders;
    final RecordManager recordManager = RecordManager.getInstance();
    private String recordDir = String.format(Locale.getDefault(), "%s/Record/driftbottle/",
            Environment.getExternalStorageDirectory().getAbsolutePath());

    //低版本选取回来的是Uri
    private ValueCallback<Uri> uploadFile;
    //高版本选取回来的是Uri数组
    private ValueCallback<Uri[]> uploadFileAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 9999;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏头部标题FLAG_LAYOUT_IN_SCREEN
        getSupportActionBar().hide();
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
         getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
         // 设置页面内容布局视图
         setContentView(R.layout.activity_main);
         // webview设置local_webview
         webview = (WebView) findViewById(R.id.local_webview); // 获取vebview元素
         WebSettings settings = webview.getSettings(); // 获取webview设置对象
         initSetWebView(settings);
         webview.addJavascriptInterface(new JsInteration(), "android");
    //     webview.loadUrl("http://192.168.45.2:3005/");
         webview.loadUrl("https://www.i8q.cn/");
         webview.setWebViewClient(new WebViewClient(){
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url){
                    Log.d("加载的url", url);
                    view.loadUrl(url);
                    jsFuntion("read", "{\"m\": \"加载成功\"}");
                    return true;
         };

    //            @Override
    //            public void onPageFinished(WebView view, String url) {
    //                Log.d("onPageFinished", "网页加载成功");
    //                //设定加载结束的操作
    //                String calljs = "javascript：androidToJS（\"read\",\"{\"m\": \"加载成功\"}\"）";
    //                webview.loadUrl(calljs);
    //            }
    //            @Override
    //            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    //                Log.d("onReceivedError-description", description);
    //                Log.d("onReceivedError-failingUrl", failingUrl);
    //            };
            });
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                Log.d("微信-ogpsp", origin);
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadFile = valueCallback;
                openImageChooserActivity();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadFile = valueCallback;
                openImageChooserActivity();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadFile = valueCallback;
                openImageChooserActivity();
            }
            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {
                uploadFileAboveL = valueCallback;
                openImageChooserActivity();
                return true;
            }
        });
        webview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        api = WXAPIFactory.createWXAPI(this, APP_ID, false);
        api.registerApp(APP_ID);
        mTencent = Tencent.createInstance("101581040", this.getApplicationContext());   //222222
//        getWindow().findViewById(getResources().getIdentifier("statusBarBackground", "id", "android")).setBackgroundResource(Color.parseColor("#47A4DD"));

//        SharedPreferences sharedPreferences = this.getSharedPreferences("share", MODE_PRIVATE);
//        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
//        if (isFirstRun) {
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putBoolean("isFirstRun", false);
//            editor.commit();
//            log("isFirstRun","第一次启动运行");
//        }
//        MyApp app = (MyApp)this.getApplication();
//        if (!app.isOpenSplash) {
//            app.isOpenSplash = true;
//            openSplashActivity();
//        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String isJsFuntion = extras.getString("jsFuntion");
            if (isJsFuntion != null) {
                String key = extras.getString("key");
                String data = extras.getString("data");
                if (isJsFuntion != null && isJsFuntion.equals("true") && key != null && data != null) {
                    jsFuntion(key, data);
                }
            }
        }
    }

    //打开选取的方法
    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadFile && null == uploadFileAboveL) return;
            Uri result = data == null || resultCode != Activity.RESULT_OK ? null : data.getData();
            if (uploadFileAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadFile != null) {
                uploadFile.onReceiveValue(result);
                uploadFile = null;
            }
        }
        if (requestCode == Constants.REQUEST_LOGIN ||
                requestCode == Constants.REQUEST_APPBAR) {
            Tencent.onActivityResultData(requestCode,resultCode,data,loginListener);
        }
    }


    @TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadFileAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadFileAboveL.onReceiveValue(results);
        uploadFileAboveL = null;
    }


    public void initSetWebView (WebSettings settings) {
        settings.setJavaScriptEnabled(true); // 允许网页运行js代码
//        settings.setSupportZoom(true);
//        settings.setDisplayZoomControls(false);//隐藏缩放图标
//        settings.setBuiltInZoomControls(true);
////        settings.setAppCacheEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
////        settings.setAppCachePath(CacheTool.getH5CachePath());
////        settings.setAppCacheMaxSize(20 * 1024 * 1024);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setGeolocationEnabled(true);
//
//        //设置加载进来的页面自适应手机屏幕
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 原因js自行执行播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        String userAgent = settings.getUserAgentString();
        JSONObject ua = new JSONObject();
        try {
            ua.put("version", BuildConfig.VERSION_NAME);
            ua.put("versionCode", BuildConfig.VERSION_CODE);
            ua.put("channel", "interior"); //interior oppo  vivo   xiaomi  360 huawei    taobao     baidu   yingyongbao  meizu  chuizi
        } catch (Exception e) {}
        settings.setUserAgentString(userAgent + "-drift-bottle -> " + ua.toString());
    }

    public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){
        callback.invoke(origin, true, false);
    }

    // 打开开屏广告
    public void openSplashActivity () {
        startActivity(new Intent(MainActivity.this, SplashActivity.class));
    }


    public class JsInteration {
        @JavascriptInterface
        public void requestPermissions(final String data) {
            Log.d("requestPermissions", data);
            try {
                JSONObject permiss = new JSONObject(data);
                JSONArray pList = permiss.getJSONArray("permission");
                String[] sArr = new String[pList.length()];
                for(int i = 0; i < pList.length(); i++) {
                    sArr[i] = pList.getString(i);
                }
                int requestCode = permiss.getInt("requestCode");
                ActivityCompat.requestPermissions(MainActivity.this, sArr, requestCode);
            } catch (JSONException e) {
                e.printStackTrace();
                jsFuntion("requestPermissions", "{\"code\":500,\"msg\":\"参数格式有误\", \"data\":\""+ data +"\"}");
            }
        }
        @JavascriptInterface
        public void qqLogin(final String data) {
            Log.d("qqLogin", data);
//            jsFuntion("qqLogin", "{\"m\":\"获取QQ信息成功\"}");
            qqLogins("1000");
        }
        @JavascriptInterface
        public void wxLogin (String data) {
            Log.d("微信登录", data);
            wxLogins(data);
        }
        @JavascriptInterface
        public void wxPay (final String data) {
            Log.d("微信支付", data);
            try {
                JSONObject option = new JSONObject(data);
                int wxSdkVersion = api.getWXAppSupportAPI();
                if (wxSdkVersion >= Build.OFFLINE_PAY_SDK_INT) {
                    PayReq req = new PayReq();
                    req.appId = option.getString("appid"); //你的微信appid
                    req.partnerId = option.getString("partnerid"); //商户号
                    req.prepayId = option.getString("prepayid"); //预支付交易会话ID
                    req.nonceStr = option.getString("noncestr"); //随机字符串
                    req.timeStamp = option.getString("timestamp"); //时间戳
                    req.packageValue = "Sign=WXPay"; //扩展字段,这里固定填写Sign=WXPay
                    req.sign = option.getString("sign");//签名
//              req.extData         = "app data"; // optional
                    api.sendReq(req);
                } else {
                    jsFuntion("wxPay", "{\"code\": 500, \"msg\": \"手机版本太低或者没有安装微信\"}");
                }
            } catch (JSONException e) {
                jsFuntion("wxPay", "{\"code\": 500, \"msg\": \"参数格式有误\"}");
            }
        }
        @JavascriptInterface
        public void alipay(final String data) {
            Log.d("支付宝支付-alipay", data);
            Runnable payRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        PayTask alipay = new PayTask(MainActivity.this);
                        JSONObject orderInfo = new JSONObject(data);
                        String aliResult = orderInfo.getString("aliResult");
                        Map<String, String> result = alipay.payV2(aliResult, true);

                        Message msg = new Message();
                        msg.what = SDK_PAY_FLAG;
                        msg.obj = result;
                        alipayHandler.sendMessage(msg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread payThread = new Thread(payRunnable);
            payThread.start();
        }
        @JavascriptInterface
        public void startRecording(final String data) {
            Log.d("录音-开始", data);
            startRecordings(data);
        }
        @JavascriptInterface
        public void endRecording(final  String data) {
            Log.d("录音-结束", data);
            endRecordings(data);
        }
        @JavascriptInterface
        public void avSplash(final String data) {
            openSplashActivity();
        }
//        @JavascriptInterface
//        public void getLocation (final String data) {
//            Log.d("getLocation", data);
//            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            List<String> providers = locationManager.getProviders(true);
////            String locationProvider;
//            String mode; // 定位模式
//            String longitude; // 经度
//            String latitude; // 纬度
//            JSONObject json = new JSONObject();
//            if (providers.contains(LocationManager.GPS_PROVIDER)) { // GPS定位
//                locationProvider = LocationManager.GPS_PROVIDER;
//                mode = "GPS";
////                json.put("mode", "GPS");
//            } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) { // 网络定位
//                locationProvider = LocationManager.NETWORK_PROVIDER;
//                mode = "NETWORK";
//            } else {
//                mode = "NO"; // 没有定位
//                return;
//            }
//            if (mode.equals("NO")) {} else {
//                Location location = locationManager.getLastKnownLocation(locationProvider);
//            }
//        }
    }

    private  Handler alipayHandler = new Handler() {
        public void handleMessage(Message msg) {
            PayResult payResult = new PayResult((Map<String, String>) msg.obj);
            Log.d("支付结果", payResult.getResult());
            jsFuntion("alipay", payResult.getResult());
        };
    };

    public void wxLogins (String data) {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "com_example_administrator_driftbottle";
        api.sendReq(req);
        Log.d("微信获取code", "snsapi_userinfo");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        user_openId = intent.getStringExtra("openId");
        accessToken = intent.getStringExtra("accessToken");
        refreshToken = intent.getStringExtra("refreshToken");
        scope = intent.getStringExtra("scope");
        Log.d("微信获取 onNewIntent - code", "user_openId: " + user_openId + ", accessToken:" + accessToken + ", refreshToken:" + refreshToken + ", scope:" + scope);
        if (accessToken != null && user_openId != null){
            NetworkUtil.sendWxAPI(wxHandler, String.format("https://api.weixin.qq.com/sns/auth?" +
                    "access_token=%s&openid=%s", accessToken, user_openId), NetworkUtil.CHECK_TOKEN);
        }
    }

    private Handler wxHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int tag = msg.what;
            Bundle data = msg.getData();
            JSONObject json =  null;
            switch (tag) {
                case NetworkUtil.CHECK_TOKEN : {
                    try {
                        json = new JSONObject(data.getString("result"));
                        int errcode = json.getInt("errcode");
                        if (errcode == 0) {
                            NetworkUtil.sendWxAPI(wxHandler, String.format("https://api.weixin.qq.com/sns/userinfo?" +
                                    "access_token=%s&openid=%s", accessToken, user_openId), NetworkUtil.GET_INFO);
                        } else {
                            NetworkUtil.sendWxAPI(wxHandler, String.format("https://api.weixin.qq.com/sns/oauth2/refresh_token?" +
                                            "appid=%s&grant_type=refresh_token&refresh_token=%s", APP_ID, refreshToken),
                                    NetworkUtil.REFRESH_TOKEN);
                        }
                    } catch (JSONException e) {
                        Log.d("wxHandler-CHECK_TOKEN", e.getMessage());
                    }
                }
                case NetworkUtil.REFRESH_TOKEN: {
                    try {
                        json = new JSONObject(data.getString("result"));
                        user_openId = json.getString("openid");
                        accessToken = json.getString("access_token");
                        refreshToken = json.getString("refresh_token");
                        scope = json.getString("scope");
                        NetworkUtil.sendWxAPI(wxHandler, String.format("https://api.weixin.qq.com/sns/userinfo?" +
                                "access_token=%s&openid=%s", accessToken, user_openId), NetworkUtil.GET_INFO);
                    } catch (JSONException e) {
                        Log.d("wxHandler-REFRESH_TOKEN", e.getMessage());
                    }
                    break;
                }
                case NetworkUtil.GET_INFO: {
                    try {
                        json = new JSONObject(data.getString("result"));
                        Log.d("微信登录成", json.toString());
                        jsFuntion("wxLogin", json.toString());
                    } catch (JSONException e) {
                        Log.d("wxHandler-GET_INFO", e.getMessage());
                    }
                }
            }
        }
    };


    // Android回调页面Js程序
    public void jsFuntion (final String key, final String data) {
        final String js = "javascript:androidToJS('"+ key +"', '"+ data +"')";
        Log.d("jsFuntion - " + key, js);
//        log("录音-返回数据长度-Log-" + data.length(),  data);
        if (Looper.getMainLooper() != Looper.myLooper()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webview.loadUrl(js);
                }
            });
        } else {
            webview.loadUrl(js);
        }
    }

    // 超长文本日志打印
    public void log (String tag, String msg) {
        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;
        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize ) {// 长度小于等于限制直接打印
            Log.e(tag, msg);
        }else {
            while (msg.length() > segmentSize ) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize );
                msg = msg.replace(logContent, "");
                Log.e(tag, logContent);
            }
            Log.e(tag, msg);// 打印剩余日志
        }
    }

    /**
     * 开始录音
     * @param data 录音配置信息
     */
    public void startRecordings (final String data) {
//        try {
//            JSONObject option = new JSONObject(data);
            recordManager.init(MyApp.getInstance(), BuildConfig.DEBUG);
            recordManager.changeFormat(RecordConfig.RecordFormat.WAV);
            recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(16000));
            recordManager.changeRecordConfig(recordManager.getRecordConfig().setEncodingConfig(AudioFormat.ENCODING_PCM_16BIT));
            log("录音-文件路径", recordDir);
            recordManager.changeRecordDir(recordDir);
            recordManager.setRecordStateListener(new RecordStateListener() {
                @Override
                public void onStateChange(RecordHelper.RecordState state) {
                    log("录音-当前-状态", state.toString());
                }
                @Override
                public void onError(String error) {
                    log("录音-状态-异常", error);
                    jsFuntion("startRecording", "{\"code\":500,\"msg\":\"开始录音失败\",\"data\":\""+ data +"\",\"e\":\"" + error + "\"}");
                }
            });
            // 监听录音结束结果
            recordManager.setRecordResultListener(new RecordResultListener() {
                @Override
                public void onResult(File result) {
                    log("录音-结束-文件路径", result.getAbsolutePath());
                    try {
                        FileInputStream wavfile = new FileInputStream(result);
                        byte[] buffer = new byte[(int)result.length()];
                        wavfile.read(buffer);
                        wavfile.close();
                        String backData = "{\"code\": 200, \"data\": {\"wav\": \"" +  Base64.encodeToString(buffer, Base64.DEFAULT) + "\"}}";
//                        log("录音-结束-结果", backData);
                        jsFuntion("endRecording", backData);
                        log("录音-结束-回调完成", "数据长度" + backData.length());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.d("录音-结束-File异常", e.getMessage());
                        jsFuntion("endRecording", "{\"code\":500,\"msg\":\"录音结束File异常\",\"data\":\""+ data +"\",\"e\":\"" + e.getMessage() + "\"}");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("录音-结束-IO异常", e.getMessage());
                        jsFuntion("endRecording", "{\"code\":500,\"msg\":\"录音结束IO异常\",\"data\":\""+ data +"\",\"e\":\"" + e.getMessage() + "\"}");
                    }
                }
            });
            recordManager.start();
//            audioRecorders = new AudioRecorder();
//            audioRecorders.start(option);
//            audioRecorders.startRecording(option, new Handler(){
//                @Override
//                public void handleMessage(Message msg){
//                    try {
//                        JSONObject backData = new JSONObject();
//                        backData.put("code", msg.what);
//                        backData.put("data", msg.obj);
//                        jsFuntion("startRecording", backData.toString());
//                    } catch (JSONException e) {
//                        jsFuntion("startRecording", "{\"code\":500,\"msg\":\"开始录音失败\",\"data\":\""+ data +"\",\"e\":\"" + e.getMessage() + "\"}");
//                    }
//                }
//            });
//        } catch (JSONException e) {
//            jsFuntion("startRecording", "{\"code\":500,\"msg\":\"开始录音失败\", \"data\":\""+ data +"\",\"e\":\"" + e.getMessage() + "\"}");
//        }
    }

    /**
     * 录音结束
     * @param data
     */
    public void endRecordings (String data) {
        log("录音-进入-结束", data);
        recordManager.stop();
//        if (audioRecorders != null) {
//            jsFuntion("endRecording", audioRecorders.endRecording());
//        }
    }

    public void doClick(View view) {
        if (!mTencent.isSessionValid()) {
            mTencent.login(this, "all", loginListener);  //all get_simple_userinfo
        }
    }

    public void qqLogins (String tel) {
        Log.d("qqLogins", "3333333333");
//        Toast.makeText(this,"aaaaa",Toast.LENGTH_LONG).show();

        if (!mTencent.isSessionValid()) {
//            mTencent.login(this, "all", loginListener,true);
            mTencent.login(this, "get_simple_userinfo", loginListener);  //all
        }

    }

    IUiListener loginListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            Log.d("SDKQQAgentPref", "AuthorSwitch_SDK:" + SystemClock.elapsedRealtime());
            Log.d("SDKQQAgentPref", values.toString());

            initOpenidAndToken(values);
            updateUserInfo(values);
//            updateLoginButton();
        }
    };

    public static void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch(Exception e) {
        }
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                JSONObject response = (JSONObject) msg.obj;
                if (response.has("nickname")) {
                    //                        mUserInfo.setVisibility(android.view.View.VISIBLE);
//                        mUserInfo.setText(response.getString("nickname"));
                }
            }else if(msg.what == 1){
                Bitmap bitmap = (Bitmap)msg.obj;
//                mUserLogo.setImageBitmap(bitmap);
//                mUserLogo.setVisibility(android.view.View.VISIBLE);
            }
        }

    };

    public void updateUserInfo(final JSONObject jsonObject) {
        if (mTencent != null && mTencent.isSessionValid()) {
            IUiListener listener = new IUiListener() {
                @Override
                public void onError(UiError e) {

                }
                @Override
                public void onComplete(final Object response) {
                    Message msg = new Message();
                    msg.obj = response;
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    new Thread(){

                        @Override
                        public void run() {
                            JSONObject json = (JSONObject)response;
                            Log.d("getUserInfo - 获取用户信息成功", json.toString());
                            JSONObject allJson = new JSONObject();
                            try {
                                allJson.put("accessToken", jsonObject);
                                allJson.put("userInfo", json);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("allJson", allJson.toString());
                            jsFuntion("qqLogin", allJson.toString());
                        }

                    }.start();
                }

                @Override
                public void onCancel() {

                }
            };
            mInfo = new UserInfo(this, mTencent.getQQToken());
            Log.d("SDKQQAgentPref", "token=" + mTencent.getQQToken());
            mInfo.getUserInfo(listener);

        } else {
//            mUserInfo.setText("");
//            mUserInfo.setVisibility(android.view.View.GONE);
//            mUserLogo.setVisibility(android.view.View.GONE);
        }
    }


    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {
            Log.d("SDKQQAgentPref", "onComplete=" + response.toString());
            if (null == response) {
//                Util.showResultDialog(MainActivity.this, "返回为空", "登录失败");
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
//                Util.showResultDialog(MainActivity.this, "返回为空", "登录失败");
                return;
            }

            //登录成功在这里传参  response.toString()  给js
//            Util.showResultDialog(MainActivity.this, response.toString(), "登录成功");
            String qqInfo = response.toString();
            Log.d("QQ登录获取票据成功", qqInfo);
//            callJs(response.toString());
            // 有奖分享处理
//            handlePrizeShare();
            doComplete((JSONObject)response);
        }

        protected void doComplete(JSONObject values) {

        }

        @Override
        public void onError(UiError e) {
            Log.d("SDKQQAgentPref", "onError=" + e.toString());
            Util.toastMessage(MainActivity.this, "onError: " + e.errorDetail);
            Util.dismissDialog();
        }

        @Override
        public void onCancel() {
            Log.d("SDKQQAgentPref", "onCancel()");
            Util.toastMessage(MainActivity.this, "onCancel: ");
            Util.dismissDialog();
            if (isServerSideLogin) {
                isServerSideLogin = false;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webview.canGoBack()) {
                webview.goBack();
                return  true;
            } else {
                System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void toVebView(View v) {
        Intent intent = new Intent(this, Test_webview.class);
        startActivity(intent);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("SDKQQAgentPref", "-->onActivityResult " + requestCode  + " resultCode=" + resultCode);
//        if (requestCode == Constants.REQUEST_LOGIN ||
//                requestCode == Constants.REQUEST_APPBAR) {
//            Tencent.onActivityResultData(requestCode,resultCode,data,loginListener);
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("录音-授权回调-成功", requestCode+" - " + permissions.toString() + " - " + grantResults.toString() + " - " + grantResults.length);
        } else {
            Log.d("录音-授权回调-失败", requestCode+" - " + permissions.toString() + " - " + grantResults.toString());
        }
        try {
            JSONObject back = new JSONObject();
            back.put("code", 200);
            back.put("msg", "授权完成");
            back.put("requestCode", requestCode);
            JSONObject obj = new JSONObject();
            for (int s = 0; s < permissions.length; s++) {
                obj.put(permissions[s], grantResults[s] == PackageManager.PERMISSION_GRANTED);
            }
            back.put("permissions", obj);
            jsFuntion("requestPermissions", back.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            jsFuntion("requestPermissions", "{\"code\":501,\"msg\":\"授权回调JSON异常\",\"data\":{\"e\":\"" + e.getMessage() + "\"}}");
        }
    }
}
