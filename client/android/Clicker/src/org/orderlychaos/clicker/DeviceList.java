package org.orderlychaos.clicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

public class DeviceList extends AsyncTask<Void, Void, String> {
	
    public static List<DeviceItem> ITEMS = new ArrayList<DeviceItem>();
    public static Map<String, DeviceItem> ITEM_MAP = new HashMap<String, DeviceItem>();
    
    private static String ERROR_NO_URL = "no URL";

    private List<DeviceItem> pendingDevices;
    private ArrayAdapter<DeviceItem> list;
    private Context context;
    private ProgressDialog progress_dialog;
    
    public DeviceList(ArrayAdapter<DeviceItem> adapter, Context context) {
    	list = adapter;
    	this.context = context;
    	pendingDevices = new ArrayList<DeviceItem>();
    }
    
    public static class DeviceItem {

        public String id;
        public Device device;

        public DeviceItem(String id, Device device) {
            this.id = id;
            this.device = device;
        }

        @Override
        public String toString() {
            return device.name;
        }
    }
        

    @Override
    protected String doInBackground(Void... params) {
    	Object[] devices = {};
    	String error = null;
    	
    	// Load our settings.
        SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
        String server_url = settings.getString("server_url", "");
        
        // If we don't have our settings yet, skip trying to load the devices.
        if (server_url.equals("")) {
        	return ERROR_NO_URL;
        }
        
    	// Connect to the server.
    	XMLRPCClient SERVER = new XMLRPCClient(server_url);
    	
    	// Get the list of devices from the server.
    	try {
      		devices = (Object[]) SERVER.call("device_list");
        } catch (XMLRPCException e) {
        	// Get the error message we should return.
        	error = DeviceListActivity.parseXMLRPCError(e, context);
       	}
    	
    	// Build the pending list of devices from the server's
    	// response.  We flush out any old items first.
    	pendingDevices.clear();
    	for (int i = 0; i< devices.length; i++) {
    		try {
    			Device device = new Device(devices[i].toString(), context);
    			pendingDevices.add(new DeviceItem(device.name, device));
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
    	ITEMS.clear();
    	ITEM_MAP.clear();
    }
    
    @Override
    protected void onPostExecute(String error) {
    	// Dismiss the progress dialog.
    	progress_dialog.dismiss();
    	
    	// Trigger the array adapter that things have changed in our list.
    	list.notifyDataSetChanged();
    	
    	// If we were passed an error, pop up a dialog
    	if (error != null) {
    		if (error.equals(ERROR_NO_URL)) {
    			// Pop up the settings dialog.
    	   		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        		final View layout = inflater.inflate(R.layout.settings_dialog, null);
        		
        		//Create and raise the dialog.
        		AlertDialog.Builder builder = new AlertDialog.Builder(context);
        		builder.setTitle(R.string.msg_enter_settings)
        			.setView(layout)
        			.setCancelable(false)
        			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int id) {
        					// Build the server URL.
        					EditText host = (EditText) layout.findViewById(R.id.server_host);
        					EditText port = (EditText) layout.findViewById(R.id.server_port);
        					String url = "http://" + host.getText() + ":" + port.getText() + "/clicker";
        					
        					// Save the URL, host, and port to our preferences.
        					SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
        				    SharedPreferences.Editor editor = settings.edit();
        				    editor.putString("server_url", url);
        				    editor.putString("server_host", host.getText().toString());
        				    editor.putInt("server_port", Integer.parseInt(port.getText().toString()));
        				    editor.commit();
        				    
        				    // Refresh the device list.
        				    ((DeviceListActivity)context).refreshDevices();
        					dialog.dismiss();
        				}
        			});
        		AlertDialog alert = builder.create();
        		alert.show();
    		} else {
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
    	} else {
    	   	// Add the devices to the list used by the UI.  We
        	// need to do this here instead of in the background
    		// thread, as only the UI thread is supposed to modify
    		// the contents of the array used by the ArrayAdapter.
    		for (int i = 0; i < pendingDevices.size(); i++) {
    			addItem(pendingDevices.get(i));
    		}

    		// Clear out the pending device list.
    		pendingDevices.clear();
    	}
    }

    private static void addItem(DeviceItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }
}
