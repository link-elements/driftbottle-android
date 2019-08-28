package com.example.administrator.driftbottle.wxapi;






import com.example.administrator.driftbottle.Constants;
import com.example.administrator.driftbottle.MainActivity;
import com.example.administrator.driftbottle.R;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler{
	
	private static final String TAG = "MicroMsg.SDKSample.WXPayEntryActivity";
	
    private IWXAPI api;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_result);
        
    	api = WXAPIFactory.createWXAPI(this, Constants.APP_ID);
        api.handleIntent(getIntent(), this);
    }

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
        api.handleIntent(intent, this);
	}

	@Override
	public void onReq(BaseReq req) {
	}

	@Override
	public void onResp(BaseResp resp) {
//		Log.d(TAG, "onPayFinish, errCode = " + resp.errCode);
		JSONObject backData = new JSONObject();
		try {
			backData.put("code", resp.errCode == 0 ? 200 : 500);
			backData.put("msg", getString(R.string.pay_result_callback_msg, String.valueOf(resp.errCode)));
			backData.put("type", resp.getType());
			backData.put("errCode", resp.errCode);
			backData.put("errStr", resp.errStr);
			backData.put("transaction", resp.transaction);
			backData.put("openId", resp.openId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent(WXPayEntryActivity.this, MainActivity.class);
		intent.putExtra("jsFuntion", "true");
		intent.putExtra("key", "wxPay");
		intent.putExtra("data", backData.toString());
		startActivity(intent);
		this.finish();

//		if (resp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setTitle(R.string.app_tip);
//			builder.setMessage(getString(R.string.pay_result_callback_msg, String.valueOf(resp.errCode)));
//			builder.show();
//		}
	}
}