package org.orderlychaos.clicker;



import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;

public class DeviceDetailFragment extends ListFragment  implements OnItemSelectedListener, OnClickListener,
			MediaPlayerListener {

    public static final String ARG_ITEM_ID = "item_id";
    
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> input_adapter;

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
    		
    		// Register switch callback.
    		power_switch.setOnClickListener(this);
    		
    		// Set the initial switch state to match the status
    		// returned by the server.
    		if (mItem.device.getStatusCmds().contains("power")) {
    			new PowerStatusTask(power_switch).execute();
    		}
    	}
    	
    	// If the device has inputs, set up an input spinner.
    	if (mItem.device.has_inputs) {
    		Spinner input_spinner = (Spinner) getActivity().
    				findViewById(R.id.device_input_spinner);
    		
    		// Make the spinner visible.
    		input_spinner.setVisibility(View.VISIBLE);
    		
    		// Populate spinner with list of inputs.
    		input_adapter = new ArrayAdapter<String>(getActivity(),
    				android.R.layout.simple_spinner_item,
    				android.R.id.text1,
    				mItem.device.getInputs()); 
    		input_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		input_spinner.setAdapter(input_adapter);
    		
    		// Set the currently selected input to match
    		// the status returned by the server.  This
    		// task will handle registering the spinner
    		// callbacks for us.
    		if (mItem.device.getStatusCmds().contains("input")) {
    			new InputStatusTask(input_spinner, this).execute();
    		}
       	}
    	
    	// If the device has media player buttons, set up the
    	// media player controls.
    	if (mItem.device.is_media_player) {
    		MediaPlayerView media_player = (MediaPlayerView) getActivity().
    				findViewById(R.id.device_media_player);
    		
    		// Make the media player controls visible.
    		media_player.setVisibility(View.VISIBLE);
    		
    		// Register our callback.
    		media_player.setMediaPlayerListener(this);
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
    
    @Override
    public void onClick(View view) {
    	// Power switch was clicked.
    	if (view.getId() == R.id.device_power_button) {
    		if (((Switch) view).isChecked()) {
    			// If the device has inputs and an input status command, set the input spinner
        		// selection as a post-task of turning the device on.
        		if (mItem.device.has_inputs && mItem.device.getStatusCmds().contains("input")) {
            		Spinner input_spinner = (Spinner) getActivity().
            				findViewById(R.id.device_input_spinner);
            		new PressButtonTask((AsyncTask<String, ?, ?>) new InputStatusTask(input_spinner, this)).execute("on");
        		} else {
        			new PressButtonTask().execute("on");
        		}
    		} else {
    			new PressButtonTask().execute("off");
    		}
    	}
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    	// Input spinner had item selected.
    	if (parent.getId() == R.id.device_input_spinner) {
    		new InputSelectTask().execute(parent.getItemAtPosition(pos).toString());
    	}
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // This is required for the spinner callbacks, but we don't use it.
    }
    
    
    // Callback for the media player controller
    public void onMediaPlayerClicked(int button) {
    	if (mItem.device.is_media_player) {
    		// Send the proper command asynchronously.
    		if (button == MediaPlayerView.PLAY) {
    			new PressButtonTask().execute("play");
    		} else if (button == MediaPlayerView.PAUSE) {
    			new PressButtonTask().execute("pause");
    		} else if (button == MediaPlayerView.STOP) {
    			new PressButtonTask().execute("stop");
    		} else if (button == MediaPlayerView.REV) {
    			new PressButtonTask().execute("rev");
    		} else if (button == MediaPlayerView.FWD) {
    			new PressButtonTask().execute("fwd");
    		} else if (button == MediaPlayerView.PREV) {
    			new PressButtonTask().execute("prev");
    		} else if (button == MediaPlayerView.NEXT) {
    			new PressButtonTask().execute("next");
    		} else if (button == MediaPlayerView.REC) {
    			new PressButtonTask().execute("rec");
    		}
    	}
    }
    
    // TODO - clear other widgets here.
    public void refreshButtons() {
    	// Find our control views.
    	Switch power_switch = (Switch) getActivity().
				findViewById(R.id.device_power_button);
    	Spinner input_spinner = (Spinner) getActivity().
    			findViewById(R.id.device_input_spinner);
    	MediaPlayerView media_player = (MediaPlayerView) getActivity().
				findViewById(R.id.device_media_player);
		
		// Make the power switch invisible.
		if (power_switch != null) {
			power_switch.setVisibility(View.GONE);
		}
		
    	// Clear the button list.
    	setListAdapter(null);
    	
    	// Make the input spinner invisible.
    	if (input_spinner != null) {
    		input_spinner.setVisibility(View.GONE);
    		input_spinner.setAdapter(null);
    	}
		
		// Make the media player controls invisible.
		media_player.setVisibility(View.GONE);
    }
    
    // Helper to send the button press asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    private class PressButtonTask extends AsyncTask<String, Void, String> {
    	private AsyncTask<String, ?, ?> post_task;
    	
    	// Basic constructor.
    	public PressButtonTask() {
    		this.post_task = null;
    	}
    	
    	// Constructor that allows another task to be executed at
    	// the end of our onPostExecute() callback.
    	public PressButtonTask(AsyncTask<String, ?, ?> post_task) {
    		this.post_task = post_task;
    	}
    	
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
        	
        	// Run our post task if one was specified.
        	if (post_task != null) {
        		post_task.execute();
        	}
        }
    }
    
    // Helper to send power status command asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    // The switch state is set accordingly.
    private class PowerStatusTask extends PressButtonTask {
    	private Switch power_switch;
    	private String result;
    	
    	public PowerStatusTask(Switch power_switch) {
    		super();
    		this.power_switch = power_switch;
    		result = null;
    	}
    	
    	protected String doInBackground(String... params) {
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
    		super.onPostExecute(error);
        	if (error == null) {
        		// Set the switch state based on the status result.
        		if ((result != null) && (result.equals("on"))) {
    				power_switch.setChecked(true);
    			} else {
    				power_switch.setChecked(false);
    			}
        	}
    	}
    }
    
    // Helper to send input status command asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    // The spinner selection is set accordingly.
    private class InputStatusTask extends PressButtonTask {
    	private Spinner input_spinner;
    	private String result;
    	DeviceDetailFragment fragment;
    	
    	public InputStatusTask(Spinner input_spinner, DeviceDetailFragment fragment) {
    		super();
    		this.input_spinner = input_spinner;
    		this.fragment = fragment;
    		result = null;
    	}
    	
    	protected String doInBackground(String... params) {
        	String error = null;
        	
        	try {
				result = mItem.device.getStatus("input");
			} catch (XMLRPCException e) {
				// Get the error message we should return.
	        	error = DeviceListActivity.parseXMLRPCError(e, getActivity());
	        }
			
			return error;
    	}
    	
    	protected void onPostExecute(String error) {
    		super.onPostExecute(error);
        	if (error == null) {
        		// Clear the spinner callbacks.
        		input_spinner.setOnItemSelectedListener(null);
        		
        		// Set the spinner selection based on the input status result.
        		input_spinner.setSelection(input_adapter.getPosition(result));
        		
        		// Set the spinner callbacks.  We do this here to prevent
        		// the above setSelection() call from triggering the callback.
        		input_spinner.setOnItemSelectedListener(fragment);
        		
        	}
    	}
    }
    
    // Helper to select an input asynchronously.  This handles
    // raising an error dialog if we have trouble talking to the server.
    // The input selection command is not set if the input is already
    //selected.
    private class InputSelectTask extends PressButtonTask {
    	protected String doInBackground(String... input) {
        	String error = null;
        	
        	try {
				if (!mItem.device.getStatus("input").equals(input[0])) {
					mItem.device.selectInput(input[0]);
				}
			} catch (XMLRPCException e) {
				// Get the error message we should return.
	        	error = DeviceListActivity.parseXMLRPCError(e, getActivity());
	        }
			
			return error;
    	}
    }
}
