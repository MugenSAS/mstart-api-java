package com.mugensas.mstart;

import com.osc.OscMessage;

/**
 * OSCMessageObserver.java
 * Interface class to automatically dispatch received OSC messages by the client from an M-START Server.
 * 
 * @since 1.0
 * @author tmehamli
 * @version 1.0
 *
 */
public interface OSCMessageObserver {
	
	/**
	 * Sets the received OSC message to the data management class to process it.
	 * <p>
	 * This method is automatically called when a new OSC message is received.
	 * 
	 * @param aMessage the received OSC message
	 */
	public void messageReceived(OscMessage aMessage);
}
