package org.orderlychaos.clicker;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

public class DeviceListActivity extends FragmentActivity
        implements DeviceListFragment.Callbacks {

    private boolean mTwoPane;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_device_list);

        if (findViewById(R.id.device_detail_container) != null) {
            mTwoPane = true;
            ((DeviceListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.device_list))
                    .setActivateOnItemClick(true);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu_bar, menu);
        return true;
    }

    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(DeviceDetailFragment.ARG_ITEM_ID, id);
            DeviceDetailFragment fragment = new DeviceDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.device_detail_container, fragment)
                    .commit();

        } else {
            Intent detailIntent = new Intent(this, DeviceDetailActivity.class);
            detailIntent.putExtra(DeviceDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	AlertDialog.Builder builder;
    	AlertDialog alert;
    	
        switch (item.getItemId()) {
        	// Bring up the activities list.
        	case R.id.activities:
        		// Get the activities list and bring up a selection dialog.
        		ActivityList activities = new ActivityList(this);
        		activities.execute();
        		return true;
        	// Bring up the server settings dialog.
        	case R.id.settings:
        		// Get our settings dialog layout.
        		final Context context = getApplicationContext();
        		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        		final View layout = inflater.inflate(R.layout.settings_dialog, null);
        		final SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
        		final EditText host = (EditText) layout.findViewById(R.id.server_host);
				final EditText port = (EditText) layout.findViewById(R.id.server_port);
				
        		//Create the dialog.
        		builder = new AlertDialog.Builder(this);
        		builder.setTitle(R.string.msg_enter_settings)
        			.setView(layout)
        			.setCancelable(true)
        			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int id) {
        					// Build the server URL.
        					String url = "http://" + host.getText() + ":" + port.getText() + "/clicker";
        					
        					// Save the URL, host, and port to our preferences.
        				    SharedPreferences.Editor editor = settings.edit();
        				    editor.putString("server_url", url);
        				    editor.putString("server_host", host.getText().toString());
        				    editor.putInt("server_port", Integer.parseInt(port.getText().toString()));
        				    editor.commit();
        				    
        				    // Get rid of the settings dialog.
        					dialog.dismiss();
        					
        					// Reload the device list.
        	            	refreshDevices();
        				}
        			})
        			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int id) {
        					dialog.dismiss();
        				}
        			});
        		alert = builder.create();
        		
        		// Pre-populate the fields with the current settings.
        		host.setText(settings.getString("server_host", ""));
        		port.setText(Integer.toString(settings.getInt("server_port", 0)));
        		
        		// Show the dialog.
        		alert.show();
        		return true;
        	// Refresh the devices from the server.
            case R.id.refresh:
            	// Reload the device list.
            	refreshDevices();
                return true;
            case R.id.power:
            	// Raise power off confirmation dialog.
            	builder = new AlertDialog.Builder(this);
        		builder.setMessage(R.string.msg_power_off)
        			.setCancelable(true)
        			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int id) {
        					// Send power off command in another thread.
        					new PowerOffTask().execute();
        					
        					// Dismiss the power off confirmation dialog.
        					dialog.dismiss();
        				}
        			})
        			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int id) {
        					dialog.dismiss();
        				}
        			});
        		alert = builder.create();
        		alert.show();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    // Power switch callback.
    public void onPowerSwitchClicked(View view) {
    	// Find the detail fragment and call it's power switch callback.
        DeviceDetailFragment detail_frag = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.device_detail_container);
    	if (detail_frag != null) {
    		detail_frag.onPowerSwitchClicked(((Switch) view).isChecked());
    	}
        
        // TODO - check device state and set toggle state if necessary?
    }
    
    // Helper to refresh the list of devices.
    public void refreshDevices() {
    	((DeviceListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.device_list))
                .refreshDevices();
    	
    	// Clear the buttons.  Our button fragment might be
    	// empty, so we need to check if it's null first.
    	DeviceDetailFragment detail_frag = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.device_detail_container);
    	if (detail_frag != null) {
    		detail_frag.refreshButtons();
    	}
    }
    
    // Helper to parse an XMLRPCException and
    // return a useful error string.
    public static String parseXMLRPCError(XMLRPCException e, Context context) {
    	// Get the error message from the exception.
    	String error = e.getMessage();
    	
    	// Map the exception message to a useful error message.
    	if (error.startsWith("HTTP status code: 404")) {
    		error = context.getString(R.string.msg_incorrect_server);
    	} else if ((error.startsWith("org.apache.http.conn.HttpHostConnectException")) ||
    			(error.startsWith("java.net.UnknownHostException"))) {
    		error = context.getString(R.string.msg_connect_error);
    	} else {
    		error = context.getString(R.string.msg_devlist_fetch_error);
    		e.printStackTrace();
    	}
    	
    	return error;
    }
    
    // Helper to send the power off command asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    private class PowerOffTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
        	String error = null;
        	
        	// Look up our settings.
        	SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
	        String server_url = settings.getString("server_url", "");
	        
	        // Send the power off command to the server.
			XMLRPCClient server = new XMLRPCClient(server_url);
			try {
				server.call("power_off");
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
