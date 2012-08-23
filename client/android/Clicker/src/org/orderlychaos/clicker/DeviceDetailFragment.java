package org.orderlychaos.clicker;



import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;

public class DeviceDetailFragment extends ListFragment {

    public static final String ARG_ITEM_ID = "item_id";
    
    private ArrayAdapter<String> adapter;

    DeviceList.DeviceItem mItem;

    public DeviceDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = DeviceList.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_device_detail, container, false);
    	
    	// Set the list contents to be the buttons for this device.
    	if (mItem != null) {
    		adapter = new ArrayAdapter<String>(getActivity(),
    				android.R.layout.simple_list_item_activated_1,
    				android.R.id.text1,
    				mItem.device.getButtons());
    		setListAdapter(adapter);
    	}
    	
    	return rootView;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	// If the device has power buttons, set up the power switch.
    	if (mItem.device.has_power) {
    		Switch power_switch = (Switch) getActivity().
    				findViewById(R.id.device_power_button);
    		
    		// Make the switch visible.
    		power_switch.setVisibility(View.VISIBLE);
    		
    		// Set the initial switch state to match the status
    		// returned by the server.
    		if (mItem.device.getStatusCmds().contains("power")) {
    			new PowerStatusTask(power_switch).execute();
    		}
    	}
    	
    	// TODO - show other widgets here.
    }
    
    // Button list callback.
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
    	if (mItem != null) {
    		// Send the button press in a separate thread.
    		new PressButtonTask().execute(l.getItemAtPosition(position).toString());
    	}
    }
    
    // Power switch callback.  This is called by the DeviceListActivity.
    public void onPowerSwitchClicked(boolean on) {
    	if (on) {
    		new PressButtonTask().execute("on");
    	} else {
    		new PressButtonTask().execute("off");
    	}
    }
    
    // TODO - clear other widgets here too
    public void refreshButtons() {
    	// Clear the power switch.
    	Switch power_switch = (Switch) getActivity().
				findViewById(R.id.device_power_button);
		
		// Make the switch invisible.
		if (power_switch != null) {
			power_switch.setVisibility(View.GONE);
		}
		
    	// Clear the button list.
    	setListAdapter(null);
    }
    
    // Helper to send the button press asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    private class PressButtonTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... button) {
        	String error = null;
        	
        	try {
				mItem.device.pressButton(button[0]);
			} catch (XMLRPCException e) {
				// Get the error message we should return.
	        	error = DeviceListActivity.parseXMLRPCError(e, getActivity());
	        }
			
			return error;
        }
        
        protected void onPostExecute(String error) {
        	if (error != null) {
        		// Pop up an error dialog.
    			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
    
    // Helper to send power status command asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    // The switch state is set accordingly.
    private class PowerStatusTask extends AsyncTask<Void, Void, String> {
    	private Switch power_switch;
    	private String result;
    	
    	public PowerStatusTask(Switch power_switch) {
    		this.power_switch = power_switch;
    		result = null;
    	}
    	
    	protected String doInBackground(Void... params) {
        	String error = null;
        	
        	try {
				result = mItem.device.getStatus("power");
			} catch (XMLRPCException e) {
				// Get the error message we should return.
	        	error = DeviceListActivity.parseXMLRPCError(e, getActivity());
	        }
			
			return error;
    	}
    	
    	protected void onPostExecute(String error) {
        	if (error != null) {
        		// Pop up an error dialog.
    			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        		// Set the switch state based on the status result.
        		if ((result != null) && (result.equals("on"))) {
    				power_switch.setChecked(true);
    			} else {
    				power_switch.setChecked(false);
    			}
        	}
    	}
    }
}
