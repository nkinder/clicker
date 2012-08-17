package org.orderlychaos.clicker;

import java.util.Arrays;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.content.Context;
import android.content.SharedPreferences;

public class Device {
	public String name;
	
	private String description;
	private String[] buttons;
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
  		Object[] button_objs = (Object[]) server.call("device_list_buttons", name);
  		this.buttons = Arrays.copyOf(button_objs, button_objs.length, String[].class);    	
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public String[] getButtons() {
		return this.buttons;
	}
	
	public void pressButton(String button) throws XMLRPCException {
		XMLRPCClient server = new XMLRPCClient(server_url);
		server.call("device_press_button", this.name, button);
	}
	
	@Override
	public String toString() {
		return "Name: " + this.name + "\nDescription: " + this.description + "\nButtons: " + Arrays.toString(this.buttons);
	}
}
