package org.orderlychaos.clicker;

import java.util.ArrayList;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.content.Context;
import android.content.SharedPreferences;

public class Device {
	public String name;
	public boolean has_power;
	
	private String description;
	private ArrayList<String> buttons;
	private ArrayList<String> status_cmds;
	private String server_url;
	
	// TODO - split buttons out into categories, such as inputs, volume,
	// power, navigation, and media_control.  This can be determined by
	// the button names.  All other buttons should go in the regular buttons
	// list.
	
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
      		
  		// Get the list of buttons for this device.
  		this.buttons = new ArrayList<String>();
  		Object[] button_objs = (Object[]) server.call("device_list_buttons", name);
  		for (int i = 0; i < button_objs.length; i++) {
  			buttons.add(button_objs[i].toString());
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
	
	public ArrayList<String> getStatusCmds() {
		return status_cmds;
	}
	
	public void pressButton(String button) throws XMLRPCException {
		XMLRPCClient server = new XMLRPCClient(server_url);
		server.call("device_press_button", this.name, button);
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
