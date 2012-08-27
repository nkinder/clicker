package org.orderlychaos.clicker;

import java.util.ArrayList;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.content.Context;
import android.content.SharedPreferences;

public class Device {
	public String name;
	public boolean has_power;
	public boolean has_inputs;
	public boolean has_navigation;
	public boolean is_media_player;
	
	private String description;
	private ArrayList<String> buttons;
	private ArrayList<String> inputs;
	private ArrayList<String> status_cmds;
	private String server_url;
	
	public Device(String name, Context context) throws XMLRPCException {
		this.name = name;
		
		// Load our settings.  We stash the URL so we don't have
		// to look it up every time a button is pressed.
        SharedPreferences settings = context.getSharedPreferences("ClickerSettings", Context.MODE_PRIVATE);
        server_url = settings.getString("server_url", "");
        
        // Connect to the server.
		XMLRPCClient server = new XMLRPCClient(server_url);

		// Get the device description.
  		this.description = ((Object) server.call("device_info", name)).toString();
      	
  		// TODO - split out numpad and volume buttons.
  		// Get the list of buttons and inputs for this device.
  		this.buttons = new ArrayList<String>();
  		this.inputs = new ArrayList<String>();
  		Object[] button_objs = (Object[]) server.call("device_list_buttons", name);
  		for (int i = 0; i < button_objs.length; i++) {
  			String button = button_objs[i].toString();
  			if (button.startsWith("input_") && (button.length() > 6)) {
  				inputs.add(button.substring(6));
  			} else {
  				buttons.add(button);
  			}
  		}
  		
  		// If we have any inputs, set a flag.
  		if (inputs.isEmpty()) {
  			has_inputs = false;
  		} else {
  			has_inputs = true;
  		}
  		
  		// If we have power on/off buttons, remove them from the list
  		// of normal buttons and create a special power switch.
  		if (buttons.contains("on") && buttons.contains("off")) {
  			buttons.remove("on");
  			buttons.remove("off");
  			has_power = true;
  		} else {
  			has_power = false;
  		}
  		
  		// If we have media player buttons, remove them from the list of
  		// normal buttons and create a special media control view.
  		if (buttons.contains("play") && buttons.contains("pause") &&
  				buttons.contains("stop") && buttons.contains("rev") &&
  				buttons.contains("fwd") && buttons.contains("prev") &&
  				buttons.contains("next")) {
  			buttons.remove("play");
  			buttons.remove("pause");
  			buttons.remove("stop");
  			buttons.remove("rev");
  			buttons.remove("fwd");
  			buttons.remove("prev");
  			buttons.remove("next");
  			is_media_player = true;
  		}
  		
  		// If we have navigation buttons, remove them from the list of
  		// normal buttons to create a navigation control.
  		if (buttons.contains("up") && buttons.contains("down") &&
  				buttons.contains("left") && buttons.contains("right") &&
  				buttons.contains("select")) {
  			buttons.remove("up");
  			buttons.remove("down");
  			buttons.remove("left");
  			buttons.remove("right");
  			buttons.remove("select");
  			has_navigation = true;
  		}
  		
  		// Get the list of status commands for this device.
  		status_cmds = new ArrayList<String>();
  		Object[] status_objs = (Object[]) server.call("device_list_status_cmds", name);
  		for (int i = 0; i < status_objs.length; i++) {
  			status_cmds.add(status_objs[i].toString());
  		}

	}
	
	public String getDescription() {
		return this.description;
	}
	
	public ArrayList<String> getButtons() {
		return buttons;
	}
	
	public ArrayList<String> getInputs() {
		return inputs;
	}
	
	public ArrayList<String> getStatusCmds() {
		return status_cmds;
	}
	
	public void pressButton(String button) throws XMLRPCException {
		XMLRPCClient server = new XMLRPCClient(server_url);
		server.call("device_press_button", this.name, button);
	}
	
	public void selectInput(String input) throws XMLRPCException {
		XMLRPCClient server = new XMLRPCClient(server_url);
		server.call("device_press_button", this.name, "input_" + input);
	}
	
	public String getStatus(String command) throws XMLRPCException {
		XMLRPCClient server = new XMLRPCClient(server_url);
		return ((Object) server.call("device_get_status", this.name, command)).toString();
	}
	
	@Override
	public String toString() {
		return "Name: " + this.name + "\nDescription: " +
				this.description + "\nButtons: " + buttons.toString();
	}
	
	public void refresh() {
		
	}
}
