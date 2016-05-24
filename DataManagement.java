package com.mugensas.mstart;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.osc.OscMessage;
import com.osc.OscMessageComposer;

import com.mugensas.mstart.ClientSocket;

/**
 * This class helps connecting to an M-START Server, managing received OSC messages from an M-START Server 
 * and facilitates sending OSC messages to an M-START Server.
 * 
 * @since 1.0
 * @author tmehamli
 * @version 1.0
 *
 */
public class DataManagement implements OSCMessageObserver{

	private ClientSocket socket = null;
	private int serverPort = 59900;
	private String serverIp = "127.0.0.1";
	private final Lock lock = new ReentrantLock();
	private OSCMessageObserver observer = null;
	private boolean initialized = true;
	
	/**
	 * Default class constructor which provides default settings to create a connection with an M-START Server.
	 * 
	 * @param anObserver the observer to notify when a new message has been received
	 * @param anActivity the activity name as it is set in the M-START Server database
	 */
	DataManagement(OSCMessageObserver anObserver, String anActivity){
		observer = anObserver;
		
		init(anActivity);
	}
	
	/**
	 * Constructor which provides connection creation to an M-START Server.
	 * 
	 * @param anObserver the observer to notify when a new message has been received
	 * @param anActivity the activity name as it is set in the M-START Server database
	 * @param serverPort the M-START Server port (default is 59900)
	 * @param serverIP the M-START Server IP address
	 */
	DataManagement(OSCMessageObserver anObserver, String anActivity, int serverPort, String serverIp){
		observer = anObserver;
		this.serverPort = serverPort;
		this.serverIp = serverIp;
		
		init(anActivity);
	}
	
	private void init(String anActivity)
	{
		if (observer == null)
			System.err.println("The observer is null, the notification mechanism is deactivated");
		
		socket = new ClientSocket(serverIp, serverPort, anActivity, this);
		socket.loopConnect(); // Start connection with the M-START Server
		
		initialized = socket.isConnected();
	}
	
	@Override
	public void messageReceived(OscMessage aMessage) {
		// Re-implement this method if you wish to process incoming OSC messages here instead of another class
		lock.lock();
		
		if (observer != null)
			observer.messageReceived(aMessage);
		else
			System.err.println("Message received but not sent because there is no valid observer.");
		
		lock.unlock();
	}
	
	/**
	 * Sends an already composed message.
	 * <p>
	 * This method sends a message made with an OSC message composer object.
	 * <p>
	 * Note: you can re-implement this method if you want to handle the message to be sent here. 
	 * 
	 * @param aMessage the OSC composer used to create the message
	 */
	public void sendMessage(OscMessageComposer aMessage) {
		socket.sendMessages(aMessage);
	}
	
	/**
	 * Sends a 32 bits integer to an M-START Server.
	 * 
	 * @param anInteger the integer to send
	 * @param OSCAddress the address of the OSC message
	 */
	public void sendInt(int anInteger, String OSCAddress) {
		OscMessageComposer c = new OscMessageComposer(OSCAddress);
		c.pushInt32(anInteger);
		socket.sendMessages(c);
	}
	
	/**
	 * Sends a 64 bits integer to an M-START Server.
	 * 
	 * @param aLong the integer to send
	 * @param OSCAddress the address of the OSC message
	 */
	public void sendLong(long aLong, String OSCAddress) {
		OscMessageComposer c = new OscMessageComposer(OSCAddress);
		c.pushInt64(aLong);
		socket.sendMessages(c);
	}

	/**
	 * Sends a float to an M-START Server.
	 * 
	 * @param aFloat the float to send
	 * @param OSCAddress the address of the OSC message
	 */
	public void sendFloat(float aFloat, String OSCAddress) {
		OscMessageComposer c = new OscMessageComposer(OSCAddress);
		c.pushFloat(aFloat);
		socket.sendMessages(c);
	}
	
	/**
	 * Sends a double to an M-START Server.
	 * 
	 * @param aDouble the double to send
	 * @param OSCAddress the address of the OSC message
	 */
	public void sendDouble(double aDouble, String OSCAddress) {
		OscMessageComposer c = new OscMessageComposer(OSCAddress);
		c.pushDouble(aDouble);
		socket.sendMessages(c);
	}

	/**
	 * Sends a string to an M-START Server.
	 * 
	 * @param aString the string to send
	 * @param OSCAddress the address of the OSC message
	 */
	public void sendString(String aString, String OSCAddress) {
		OscMessageComposer c = new OscMessageComposer(OSCAddress);
		c.pushString(aString);
		socket.sendMessages(c);
	}

	/**
	 * Returns true if the socket is initialized and connected.
	 * <p>
	 * The method might return false if the socket was connecting. Do not rely totally on the status of 
	 * the socket based solely on this method.
	 * 
	 * @return true if the the socket is fully initialized
	 */
	public boolean isInit() {
		return initialized;
	}
}
