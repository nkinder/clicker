package org.orderlychaos.clicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class NavigationView extends TableLayout implements OnClickListener {
	public static int UP = 1;
	public static int DOWN = 2;
	public static int LEFT = 3;
	public static int RIGHT = 4;
	public static int SELECT = 5;
	
	private NavigationListener listener;
	private ImageButton up_button;
	private ImageButton down_button;
	private ImageButton left_button;
	private ImageButton right_button;
	private ImageButton select_button;

	public NavigationView(Context context) {
		this(context, (AttributeSet)null);
	}

	public NavigationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Initialize our listener.
		listener = null;
		
		// Create our buttons.
		select_button = new ImageButton(context);
		select_button.setImageResource(R.drawable.navigation_select);
		select_button.setOnClickListener(this);
		
		up_button = new ImageButton(context);
		up_button.setImageResource(R.drawable.navigation_up);
		up_button.setOnClickListener(this);
		
		down_button = new ImageButton(context);
		down_button.setImageResource(R.drawable.navigation_down);
		down_button.setOnClickListener(this);
		
		left_button = new ImageButton(context);
		left_button.setImageResource(R.drawable.navigation_left);
		left_button.setOnClickListener(this);

		right_button = new ImageButton(context);
		right_button.setImageResource(R.drawable.navigation_right);
		right_button.setOnClickListener(this);
	
		// Create our table rows.
		TableRow top_row = new TableRow(context);
		TableRow middle_row = new TableRow(context);
		TableRow bottom_row = new TableRow(context);
		
		// Create some dummy views for our empty leading cells.
		TextView empty1 = new TextView(context);
		TextView empty2 = new TextView(context);
		
		// Add our buttons to the rows.
		top_row.addView(empty1);
		top_row.addView(up_button);
		middle_row.addView(left_button);
		middle_row.addView(select_button);
		middle_row.addView(right_button);
		bottom_row.addView(empty2);
		bottom_row.addView(down_button);
		
		// Add our rows to the table.
		addView(top_row);
		addView(middle_row);
		addView(bottom_row);
	}
	
	// Registers a callback listener.
	public void setNavigationListener(NavigationListener l) {
		listener = l;
	}
	
	// Our internal on-click listener for the buttons.  This just
	// determines what button was clicked and calls the registered
	// callback.
	@Override
	public void onClick(View view) {
		if (listener != null) {
			if (view == up_button) {
				listener.onNavigationClicked(UP);
			} else if (view == down_button) {
				listener.onNavigationClicked(DOWN);
			} else if (view == left_button) {
				listener.onNavigationClicked(LEFT);
			} else if (view == right_button) {
				listener.onNavigationClicked(RIGHT);
			} else if (view == select_button) {
				listener.onNavigationClicked(SELECT);
			}
		}
	}

}
