package org.orderlychaos.clicker;

import java.util.ArrayList;
import java.util.List;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;

public class ActivityList extends AsyncTask<Void, Void, String> {
	private List<String> activities;
	private List<String> descriptions;
	private String current_activity;
	private Context context;
	private ProgressDialog progress_dialog;
	
	public ActivityList(Context context) {
		this.context = context;
		activities = new ArrayList<String>();
		descriptions = new ArrayList<String>();
		current_activity = "";
	}
	
    @Override
    protected String doInBackground(Void... params) {
    	Object[] activity_list = {};
    	String error = null;
    	
    	// Load our settings.
        SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
        String server_url = settings.getString("server_url", "");
        
    	// Connect to the server.
    	XMLRPCClient SERVER = new XMLRPCClient(server_url);
    	
    	// Get the activity information from the server.
    	try {
      		activity_list = (Object[]) SERVER.call("activity_list");
      		current_activity = SERVER.call("activity_current").toString();
        } catch (XMLRPCException e) {
        	// Get the error message we should return.
        	error = DeviceListActivity.parseXMLRPCError(e, context);
       	}
    	
    	// Build the list of activities from the server's response.  We flush out any old items first.
    	activities.clear();
    	for (int i = 0; i< activity_list.length; i++) {
    		try {
    			// We create two lists in the same order.  We do this instead of using a Map
    			// since we need an array of the descriptions to display in the chooser dialog
    			// in our postExecute method.
    			activities.add(activity_list[i].toString());
    			descriptions.add(SERVER.call("activity_info", activity_list[i].toString()).toString());
    		} catch (XMLRPCException e) {
    			// Get the error message we should return.
            	error = DeviceListActivity.parseXMLRPCError(e, context);
    		}
    	}
    	
    	return error;
    }
    
    @Override
    protected void onPreExecute() {
    	// Start a progress dialog to indicate we're loading data.
    	progress_dialog = ProgressDialog.show(context, "", 
                context.getString(R.string.msg_loading), true);
    	
    	// Flush out the old list items.
    	activities.clear();
    	
    }
    
    @Override
    protected void onPostExecute(String error) {
    	// Dismiss the progress dialog.
    	progress_dialog.dismiss();
    	
    	// If we were passed an error, pop up an error dialog.
    	if (error != null) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(context);
    		builder.setMessage(error)
    	       .setCancelable(false)
    	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.dismiss();
    	           }
    	       });
    		AlertDialog alert = builder.create();
    		alert.show();
    	} else {
    		// Pop up an activity selection dialog.
    		AlertDialog.Builder builder = new AlertDialog.Builder(context);
    		builder.setTitle(R.string.msg_choose_activity);
    		builder.setSingleChoiceItems(descriptions.toArray(new CharSequence[0]),
    				activities.indexOf(current_activity), new DialogInterface.OnClickListener() {
    		    public void onClick(DialogInterface dialog, int item) {
    		    	// Send the start activity command in a separate thread.
    		    	new StartActivityTask().execute(activities.get(item));
    		    	
    		    	// Dismiss the activity selection dialog.
    		    	dialog.dismiss();
    		    }
    		});
    		AlertDialog alert = builder.create();
    		alert.show();
    	}
    	
    }
    
    // Helper task to send start command asynchronously.  This also handles
    // raising an error dialog if we have trouble talking to the server.
    private class StartActivityTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... activity) {
        	String error = null;
        	
        	// Load our settings.
        	SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
	        String server_url = settings.getString("server_url", "");
	        
	        // Send the command to start the activity.
			XMLRPCClient server = new XMLRPCClient(server_url);
			try {
				server.call("activity_start", activity[0]);
			} catch (XMLRPCException e) {
				// Get the error message we should return.
	        	error = DeviceListActivity.parseXMLRPCError(e, context);
	        }
			
			return error;
        }
        
        protected void onPostExecute(String error) {
        	if (error != null) {
        		// Pop up an error dialog.
    			AlertDialog.Builder builder = new AlertDialog.Builder(context);
    			builder.setMessage(error)
    				.setCancelable(false)
    				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int id) {
    						dialog.dismiss();
    					}
    				});
    			AlertDialog alert = builder.create();
    			alert.show();
        	}
        }
    }

}
