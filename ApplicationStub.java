package com.mugensas.mstart;

import com.osc.OscMessage;
import com.osc.exception.OscBadDataTypeRequestException;

/**
 * This class is an application stub using the content of this API to dialog with an M-START Server.
 * 
 * @since 1.0
 * @author tmehamli
 * @version 1.0
 *
 */
public class ApplicationStub implements OSCMessageObserver {

	private ApplicationStub() {
		// Initialize anything you need here
	}
	
	@Override
	public void messageReceived(OscMessage aMessage) {
		// Handle the incoming messages here
		// All incoming messages from the server will have their address begining with /client-send/
		// Parse the address to process the OSC message
		String address = null;
		try {
			address = aMessage.getValue(0).getAddress(); // getValue(0) returns the address
			// System.out.println("Address received: " + address);
		} catch (OscBadDataTypeRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (address != null)
		{
			if (address.startsWith("/client-send/..."))
			{
				// Start processing the address
				// Access values in the message
				// For an int: int i = aMessage.getValue(2).getInt32(); getValue(2) returns the first data in the OSC message
			}
			else // Complete your process
			{
				
			}
		} // address != null
	}

	public static void main(String[] args) {
		final ApplicationStub as = new ApplicationStub();
		DataManagement dm = new DataManagement(as, "Activity", 59900, "192.168.0.1"); // Replace here Activity
		
		if (dm.isInit())
			System.out.println("Entering main loop");
		
		while (true) // main loop
			;

	}

}
