package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.youtube.Video;
import com.sunlightlabs.android.youtube.YouTube;
import com.sunlightlabs.android.youtube.YouTubeException;

public class LegislatorYouTube extends ListActivity {
	private static final int MENU_WATCH = 0;
	private static final int MENU_COPY = 1;
	
	private String username;
	private Video[] videos;
	
	private LoadVideosTask loadVideosTask = null;
	
	private Button refresh;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.youtube_list);
    	
    	username = getIntent().getStringExtra("username");
    	
    	LegislatorYouTubeHolder holder = (LegislatorYouTubeHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		videos = holder.videos;
    		loadVideosTask = holder.loadVideosTask;
    		if (loadVideosTask != null)
    			loadVideosTask.onScreenLoad(this);
    	}
    	
    	setupControls();
    	if (loadVideosTask == null)
    		loadVideos();
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	LegislatorYouTubeHolder holder = new LegislatorYouTubeHolder();
    	holder.videos = this.videos;
    	holder.loadVideosTask = this.loadVideosTask;
    	return holder;
    }
    
	protected void displayVideos() {
		displayVideos(false);
	}
	
    protected void displayVideos(boolean cancelled) {
    	if (videos != null) {
	    	setListAdapter(new VideoAdapter(LegislatorYouTube.this, videos));
	    	
	    	if (videos.length <= 0) {
	    		TextView empty = (TextView) findViewById(R.id.youtube_empty);
	    		empty.setText(R.string.youtube_empty);
	    		refresh.setVisibility(View.VISIBLE);
	    	}
    	} else {
    		if (!cancelled)
    			((TextView) findViewById(R.id.youtube_empty)).setText(R.string.connection_failed);
    		refresh.setVisibility(View.VISIBLE);
    	}
    }
	
	protected void loadVideos() {
	    if (videos == null)
    		loadVideosTask = (LoadVideosTask) new LoadVideosTask(this).execute(username);
    	else
    		displayVideos();
	}
	
	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		Video video = (Video) parent.getItemAtPosition(position);
		launchVideo(video);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, MENU_WATCH, 0, "Watch");
		menu.add(0, MENU_COPY, 1, "Copy link");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Video video = (Video) getListView().getItemAtPosition(info.position);
		
		switch (item.getItemId()) {
		case MENU_WATCH:
			launchVideo(video);
			return true;
		case MENU_COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
			cm.setText(video.url);
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchVideo(Video video) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(video.url)));
	}
	
	private void setupControls() {
		refresh = (Button) findViewById(R.id.youtube_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				videos = null;
				loadVideos();
			}
		});
    	registerForContextMenu(getListView());
	}
    
    protected class VideoAdapter extends ArrayAdapter<Video> {
    	LayoutInflater inflater;

        public VideoAdapter(Activity context, Video[] videos) {
            super(context, 0, videos);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			if (convertView == null)
				view = (LinearLayout) inflater.inflate(R.layout.youtube, null);
			else
				view = (LinearLayout) convertView;
			
			Video video = getItem(position);
			((TextView) view.findViewById(R.id.video_title)).setText(video.title);
			((TextView) view.findViewById(R.id.video_description)).setText(Utils.truncate(video.description, 150));
			((TextView) view.findViewById(R.id.video_when)).setText(video.timestamp.format("%b %d"));
			
			return view;
		}
    }
    
    private class LoadVideosTask extends AsyncTask<String,Void,Video[]> {
    	public LegislatorYouTube context;
    	private ProgressDialog dialog = null;
    	
    	public LoadVideosTask(LegislatorYouTube context) {
    		super();
    		this.context = context;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		loadingDialog();
    	}
    	
    	public void onScreenLoad(LegislatorYouTube context) {
    		this.context = context;
    		loadingDialog();
    	}
    	
    	@Override
    	protected Video[] doInBackground(String... usernames) {
    		try {
        		return new YouTube().getVideos(username);
        	} catch(YouTubeException e) {
        		return null;
        	}
    	}
    	
    	@Override
    	protected void onPostExecute(Video[] videos) {
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		context.videos = videos;
    		
    		context.displayVideos();
    		
    		context.loadVideosTask = null;
    	}
    	
    	private void loadingDialog() {
        	dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Plucking videos from the air...");
            
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
					context.displayVideos(true);
				}
			});
            
            dialog.show();
        }
    }
    
    static class LegislatorYouTubeHolder {
		Video[] videos;
		LoadVideosTask loadVideosTask;
	}
}