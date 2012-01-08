package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.sunlightlabs.android.congress.tasks.LoadBillTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;

public class BillLoader extends Activity implements LoadBillTask.LoadsBill {
	private LoadBillTask loadBillTask;
	private String id, code;
	private Intent intent;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.loading_fullscreen);
		
		Intent i = getIntent();
		id = i.getStringExtra("id");
		code = i.getStringExtra("code");
		intent = (Intent) i.getParcelableExtra("intent");
		
		// if coming from a shortcut intent, there appears to be a bug with packaging sub-intents
		// and the intent will be null
		if (intent == null)
			intent = Utils.billPagerIntent();
		
		loadBillTask = (LoadBillTask) getLastNonConfigurationInstance();
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else
			loadBillTask = (LoadBillTask) new LoadBillTask(this, id).execute("basic", "sponsor", "latest_upcoming");
		
		setupControls();
	}
	
	
	public void setupControls() {
		if (code != null && !code.equals(""))
			Utils.setTitle(this, Bill.formatCode(code));
		Utils.setLoading(this, R.string.bill_loading);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadBillTask;
	}
	
	public void onLoadBill(Bill bill) {
		intent.putExtra("bill", bill);
		// pass entry info along, this loader class is an implementation detail
		startActivity(Analytics.passEntry(this, intent));
		finish();
	}
	
	public void onLoadBill(CongressException exception) {
		if (exception instanceof CongressException.NotFound)
			Utils.alert(this, R.string.bill_not_found);
		else
			Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public Context getContext() {
		return this;
	}

}