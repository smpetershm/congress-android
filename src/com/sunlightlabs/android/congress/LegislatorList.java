package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends ListActivity {
	private ProgressDialog dialog, shortcutDialog;
	
	private final static int SEARCH_ZIP = 0;
	private final static int SEARCH_LOCATION = 1;
	private final static int SEARCH_STATE = 2;
	private final static int SEARCH_LASTNAME = 3;
	
	private Legislator[] legislators = null;
	private LoadLegislatorsTask loadLegislatorsTask = null;
	private ShortcutImageTask shortcutImageTask = null;
	private Button back, refresh;
	
	// whether the user has come to this activity looking to create a shortcut
	private boolean shortcut;
	
	private String zipCode, lastName, state, api_key;
	private double latitude = -1;
	private double longitude = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.legislator_list);
    	
		Bundle extras = getIntent().getExtras();
		
    	zipCode = extras.getString("zip_code");
    	latitude = extras.getDouble("latitude");
    	longitude = extras.getDouble("longitude");
    	lastName = extras.getString("last_name");
    	state = extras.getString("state");

    	api_key = getResources().getString(R.string.sunlight_api_key);
    	
    	shortcut = extras.getBoolean("shortcut", false);
    	
    	setupControls();    	
    	
    	LegislatorListHolder holder = (LegislatorListHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		legislators = holder.legislators;
    		loadLegislatorsTask = holder.loadLegislatorsTask;
    		shortcutImageTask = holder.shortcutImageTask;
    	}
    	
    	if (loadLegislatorsTask == null && shortcutImageTask == null)
			loadLegislators();
    	else {
    		if (loadLegislatorsTask != null) {
    			loadLegislatorsTask.context = this;
    			loadingDialog();
    		}
    		if (shortcutImageTask != null) {
    			shortcutImageTask.context = this;
    			shortcutDialog();
    		}
    	}
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	LegislatorListHolder holder = new LegislatorListHolder();
    	holder.legislators = this.legislators;
    	holder.loadLegislatorsTask = this.loadLegislatorsTask;
    	holder.shortcutImageTask = this.shortcutImageTask;
    	return holder;
    }
    
    public void loadLegislators() {
    	if (legislators == null)
    		new LoadLegislatorsTask(this).execute();
    	else
    		displayLegislators();
    }
    
    public void displayLegislators() {
    	setListAdapter(new ArrayAdapter<Legislator>(this, android.R.layout.simple_list_item_1, legislators));
    	TextView empty = (TextView) this.findViewById(R.id.empty_msg);
    	
    	if (legislators.length <= 0) {
    		switch (searchType()) {
	    		case SEARCH_ZIP:
	    			empty.setText(R.string.empty_zipcode);
	    			break;
	    		case SEARCH_LOCATION:
	    			empty.setText(R.string.empty_location);
	    			break;
	    		case SEARCH_LASTNAME:
	    			empty.setText(R.string.empty_last_name);
	    			break;
	    		case SEARCH_STATE:
	    			empty.setText(R.string.empty_state);
	    			break;
	    		default:
	    			empty.setText(R.string.empty_general);
    		}
    		back.setVisibility(View.VISIBLE);
    	}
    }
    
    public void setupControls() {
    	back = (Button) findViewById(R.id.empty_back);
    	back.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
    	refresh = (Button) findViewById(R.id.empty_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadLegislators();
			}
		});
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	selectLegislator((Legislator) parent.getItemAtPosition(position));
    }
    
    public void selectLegislator(Legislator legislator) {
    	if (shortcut) {
    		new ShortcutImageTask(this, legislator).execute();
    	} else {
    		String legislatorId = legislator.getId();
        	Intent legislatorIntent = legislatorIntent(legislatorId);
    		startActivity(legislatorIntent);
    	}
    }
    
    private int searchType() {
    	if (zipSearch())
    		return SEARCH_ZIP;
    	else if (locationSearch())
    		return SEARCH_LOCATION;
    	else if (lastNameSearch())
    		return SEARCH_LASTNAME;
    	else if (stateSearch())
    		return SEARCH_STATE;
    	else
    		return SEARCH_LOCATION;
    }
    
    private boolean zipSearch() {
    	return zipCode != null;
    }
    
    private boolean locationSearch() {
    	// sucks for people at the intersection of the equator and prime meridian
    	return (latitude != 0.0 && longitude != 0.0);
    }
    
    private boolean lastNameSearch() {
    	return lastName != null;
    }
    
    private boolean stateSearch() {
    	return state != null;
    }
    
    public Intent legislatorIntent(String id) {
    	Intent intent = new Intent(Intent.ACTION_MAIN);
    	intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorLoader");
		
		Bundle extras = new Bundle();
		extras.putString("legislator_id", id); 
		intent.putExtras(extras);
		
		return intent;
    }
    
    public void returnShortcutIcon(Legislator legislator, Bitmap shortcutIcon) {
    	String legislatorId = legislator.getId();
    	Intent legislatorIntent = legislatorIntent(legislatorId);
		legislatorIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, legislatorIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, legislator.getProperty("lastname"));
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, shortcutIcon);
		
		setResult(RESULT_OK, intent);
		finish();
    }
    
    public void loadingDialog() {
    	dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Finding legislators...");
        dialog.show();
    }
    
    public void shortcutDialog() {
    	shortcutDialog = new ProgressDialog(this);
    	shortcutDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	shortcutDialog.setMessage("Creating shortcut...");
    	shortcutDialog.show();
    }
    
    private class ShortcutImageTask extends AsyncTask<Void,Void,Bitmap> {
    	public LegislatorList context;
    	public Legislator legislator;
    	
    	public ShortcutImageTask(LegislatorList context, Legislator legislator) {
    		super();
    		this.legislator = legislator;
    		this.context = context;
    		this.context.shortcutImageTask = this;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		context.shortcutDialog();
    	}
    	
    	@Override
    	protected Bitmap doInBackground(Void... nothing) {
    		return LegislatorProfile.shortcutImage(legislator.getId(), context);
    	}
    	
    	@Override
    	protected void onPostExecute(Bitmap shortcutIcon) {
    		if (context.shortcutDialog != null && context.shortcutDialog.isShowing())
    			context.shortcutDialog.dismiss();
    		
    		context.returnShortcutIcon(legislator, shortcutIcon);
    		
    		context.shortcutImageTask = null;
    	}
    }
    
    private class LoadLegislatorsTask extends AsyncTask<Void,Void,Legislator[]> {
    	public LegislatorList context;
    	
    	public LoadLegislatorsTask(LegislatorList context) {
    		super();
    		this.context = context;
    		this.context.loadLegislatorsTask = this;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		context.loadingDialog();
    	}
    	
    	@Override
    	protected Legislator[] doInBackground(Void... nothing) {
    		ApiCall api = new ApiCall(api_key);
			
			try {
				Map<String,String> params;
				switch (searchType()) {
					case SEARCH_ZIP:
			    		return Legislator.getLegislatorsForZipCode(api, zipCode);
					case SEARCH_LOCATION:
			    		return Legislator.getLegislatorsForLatLong(api, latitude, longitude);
					case SEARCH_LASTNAME:
			    		params = new HashMap<String,String>();
			    		params.put("lastname", lastName);
			    		return Legislator.allLegislators(api, params);
					case SEARCH_STATE:
			    		params = new HashMap<String,String>();
			    		params.put("state", state);
			    		return Legislator.allLegislators(api, params);
			    	default:
			    		return new Legislator[0];
				}
		    	
			} catch(IOException e) {
				return new Legislator[0];
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Legislator[] legislators) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		
    		context.legislators = legislators;
    		if (context.legislators != null) {
    			// if there's only one result, don't even make them click it
            	if (legislators.length == 1) {
            		context.selectLegislator(legislators[0]);
            		
            		// if we're going on to the profile of a legislator, we want to cut the list out of the stack
            		// but if we're generating a shortcut, the shortcut process will be spawning off 
            		// a separate background thread, that needs a live activity while it works, 
            		// and will call finish() on its own
            		if (!shortcut) 
            			context.finish();
            	} else
            		context.displayLegislators();
    		} else {
    			context.setListAdapter(new ArrayAdapter<Legislator>(LegislatorList.this, android.R.layout.simple_list_item_1, legislators)); 
            	((TextView) context.findViewById(R.id.empty_msg)).setText(R.string.connection_failed);
            	context.refresh.setVisibility(View.VISIBLE);
    		}
    		context.loadLegislatorsTask = null;
    	}
    }
    
    static class LegislatorListHolder {
    	Legislator[] legislators;
    	LoadLegislatorsTask loadLegislatorsTask;
    	ShortcutImageTask shortcutImageTask;
    }
}