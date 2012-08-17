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
    public void onListItemClick(ListView l, View v, int position, long id) {
    	if (mItem != null) {
    		// Send the button press in a separate thread.
    		new PressButtonTask().execute(l.getItemAtPosition(position).toString());
    	}
    }
    
    public void refreshButtons() {
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
}
