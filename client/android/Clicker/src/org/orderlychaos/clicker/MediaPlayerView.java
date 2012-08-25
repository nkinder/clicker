package org.orderlychaos.clicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MediaPlayerView extends LinearLayout implements OnClickListener {
	public static int PLAY = 1;
	public static int PAUSE = 2;
	public static int STOP = 3;
	public static int REV = 4;
	public static int FWD = 5;
	public static int PREV = 6;
	public static int NEXT = 7;
	public static int REC = 8;
	
	private MediaPlayerListener listener;
	private ImageButton play_button;
	private ImageButton pause_button;
	private ImageButton stop_button;
	private ImageButton rev_button;
	private ImageButton fwd_button;
	private ImageButton prev_button;
	private ImageButton next_button;
	private ImageButton record_button;
	
	public MediaPlayerView(Context context) {
		this(context, (AttributeSet)null, false);
	}
	
	public MediaPlayerView(Context context, boolean has_record) {
		this(context, (AttributeSet)null, has_record);
	}
	
	public MediaPlayerView(Context context, AttributeSet attrs) {
		this(context, attrs, false);
	}
	
	public MediaPlayerView(Context context, AttributeSet attrs, boolean has_record) {
		super(context, attrs);
		this.setOrientation(HORIZONTAL);
		
		// Initialize our listener.
		listener = null;
		
		// Create our buttons.
		prev_button = new ImageButton(context);
		prev_button.setImageResource(R.drawable.media_view_prev);
		prev_button.setOnClickListener(this);
		addView(prev_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		rev_button = new ImageButton(context);
		rev_button.setImageResource(R.drawable.media_view_rev);
		rev_button.setOnClickListener(this);
		addView(rev_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		stop_button = new ImageButton(context);
		stop_button.setImageResource(R.drawable.media_view_stop);
		stop_button.setOnClickListener(this);
		addView(stop_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		play_button = new ImageButton(context);
		play_button.setImageResource(R.drawable.media_view_play);
		play_button.setOnClickListener(this);
		addView(play_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		pause_button = new ImageButton(context);
		pause_button.setImageResource(R.drawable.media_view_pause);
		pause_button.setOnClickListener(this);
		addView(pause_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		fwd_button = new ImageButton(context);
		fwd_button.setImageResource(R.drawable.media_view_fwd);
		fwd_button.setOnClickListener(this);
		addView(fwd_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		next_button = new ImageButton(context);
		next_button.setImageResource(R.drawable.media_view_next);
		next_button.setOnClickListener(this);
		addView(next_button, new LinearLayout.LayoutParams(	
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		record_button = new ImageButton(context);
		record_button.setImageResource(R.drawable.media_view_rec);
		record_button.setOnClickListener(this);
		addView(record_button, new LinearLayout.LayoutParams(	
	            LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		
		if (!has_record) {
			record_button.setVisibility(GONE);
		}
		
	}
	
	// Registers a callback listener.
	public void setMediaPlayerListener(MediaPlayerListener l) {
		listener = l;
	}
	
	// Toggles the record button visibility.
	public void showRecord(boolean show) {
		if (show) {
			record_button.setVisibility(VISIBLE);
		} else {
			record_button.setVisibility(GONE);
		}
	}
	
	// Our internal on-click listener for the buttons.  This just
	// determines what button was clicked and calls the registered
	// callback.
	@Override
	public void onClick(View view) {
		if (listener != null) {
			if (view == play_button) {
				listener.onMediaPlayerClicked(PLAY);
			} else if (view == pause_button) {
				listener.onMediaPlayerClicked(PAUSE);
			} else if (view == stop_button) {
				listener.onMediaPlayerClicked(STOP);
			} else if (view == rev_button) {
				listener.onMediaPlayerClicked(REV);
			} else if (view == fwd_button) {
				listener.onMediaPlayerClicked(FWD);
			} else if (view == prev_button) {
				listener.onMediaPlayerClicked(PREV);
			} else if (view == next_button) {
				listener.onMediaPlayerClicked(NEXT);
			} else if (view == record_button) {
				listener.onMediaPlayerClicked(REC);
			}
		}
	}
}
