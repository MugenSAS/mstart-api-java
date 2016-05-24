package com.mugensas.mstart;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.osc.OscMessage;
import com.osc.OscMessageComposer;
import com.osc.OscReader;
import com.osc.OscVersion;
import com.osc.exception.OscBadDataTypeRequestException;
import com.osc.exception.OscMalformedBundleException;
import com.osc.exception.OscMalformedMessageException;

import com.mugensas.mstart.OSCMessageObserver;

/**
 * This class manages connection to an M-START Server and sending and receiving OSC messages from an M-START Server.
 * 
 * @since 1.0
 * @author tmehamli
 * @version 1.0
 *
 */
public class ClientSocket {
	
	private boolean connecting = false, reading = false, connected = false;
	private String activity; // This is the name of the activity which links Users and HMI
	private String IP; // M-START Server IP
	private int port; // M-START Server Port, default is 59900
	private Socket socket; // This client's socket
	private OutputStream socketOutput; // Socket's output 
	private InputStream socketInput; // Socket's input
	public static final String linkActivity = "/set-link-with-activity"; // This is base address of the OSC message which tells M-START Server which activity will be used
	public static final int timeDelay = 20000;
	OSCMessageObserver observer; // The data management class implements OSCMessageObserver and is notified when a new OSC message is received. 

	/**
	 * Class constructor specifying the connection information to an M-START Server.
	 * 
	 * @param IP M-START Server IP
	 * @param port M-START Server port
	 * @param activity the activity name as it is referenced in the M-START Server database
	 * @param observer the data management class to notify when an OSC message from the server is received
	 */
	public ClientSocket(String IP, int port, String activity, OSCMessageObserver observer){
		socket = null;
		this.IP = IP;
		this.port= port;
		this.activity = activity;
		this.observer = observer;
		if (this.observer == null)
			System.err.println("The observer is null, the notification mechanism is deactivated");
	}
	
	/**
	 * Returns true if the socket is connecting to an M-START Server.
	 * <p>
	 * Synchronized method needed to know if a Runnable is already trying to connect to an M-START Server.
	 * 
	 * @return true if the socket is connecting to an M-START Server
	 */
	public synchronized boolean isConnecting() {
		return connecting;
	}
	
	/**
	 * Sets the status of this socket when connecting to an M-START Server
	 * <p>
	 * Synchronized method setting a boolean to true if a Runnable is trying to connect to an M-START Server.
	 * 
	 * @param connecting boolean this socket's connection status
	 */
	public synchronized void setConnecting(boolean connecting) {
		this.connecting = connecting;
	}
	
	/**
	 * Returns true if this socket is reading from the server.
	 * <p>
	 * Synchronized method needed to know if a Runnable is already trying to read from an M-START Server.
	 * 
	 * @return true if the socket is reading from an M-START Server
	 */
	public synchronized boolean  isReading() {
		return reading;
	}
	
	/**
	 * Sets the status of this socket when reading from an M-START Server.
	 * <p>
	 * Synchronized method setting a boolean to true if a Runnable is trying to read from an M-START Server.
	 * 
	 * @param value boolean
	 */
	public synchronized void setReading(boolean reading) {
		this.reading = reading;
	}

	/**
	 * Returns true if this socket is ready to send or receive.
	 * 
	 * @return boolean true if the socket is ready
	 */
	public boolean isReady() {
		if (socket == null || socket.isClosed())
			return false;
		return true;
	}

	/**
	 * Sends the OscMessage or initiates the connection if it wasn't done yet.
	 * 
	 * @param anOSCMsg the OSC message to send
	 */
	public void sendMessages(OscMessageComposer anOSCMsg){
		try {
				byte[] len = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(anOSCMsg.getBytes(OscVersion.OSC_11).length).array();
				socketOutput.write(len); // First always send the size of the message
				socketOutput.write(anOSCMsg.getBytes(OscVersion.OSC_11)); // Then send the message
		} catch (Exception e) {
			System.out.println(e);
			if (!isConnecting()) {
				System.err.println("Server not connected, trying to connect.");
				socket = null;
				loopConnect();
			}
		}
	}
	
	/**
	 * Closes this socket and the I/O streams.
	 */
	public void close() {
		try {
			reading = false;
			if (socketOutput != null)
				socketOutput.close();
			if (socketInput != null)
				socketInput.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			System.err.println("Error closing the socket and/or the streams.");
		}
	}

	/**
	 * Loops to connect to an M-START Server each 20 seconds until the connection with this socket is made.
	 */
	public void loopConnect() {
		Runnable loopConnection = new Runnable() {
			@Override
			public void run() {
				setConnecting(true); // Change connection status in case of a new loop
				setConnected(false);
				while (!isReady()) { // Loop until the socket is ready
					try {
						connect();
						setConnecting(false);
					} catch (IOException e) {
						System.out.println("Connection failed, retrying ...");
						try {
							Thread.sleep(timeDelay); // Sleep fo 20s so every OS dependent stuff regaring the network is released
						} catch (InterruptedException e1) {
							System.err.println("Error caught while sleeping..."); // This is bad if it happens
						}
					}
				}
			}
		};
		Thread t = new Thread(loopConnection);
		t.start();
	}
	
	/**
	 * Loops to receive OSC messages from an M-START Server and notify the data management class when one is
	 * fully received.
	 * <p>
	 * This method does not handle OSC bundles as raw messages are dispatched as OSC messages. To do so, 
	 * extend or modify this method.
	 */
	public void receive() {
		
		Runnable loopReadSocket = new Runnable() {
			@Override
			public void run() {
				byte[] rawMessage = null;
				DataInputStream dis = new DataInputStream(socketInput);
				int messageLength = 0;
				OscReader reader = null;
				OscMessage message = null;
				
				while (true) {
					try {
							if (dis.available() > 3) // alignment is on 4 bytes
							{
								messageLength = dis.readInt();
							}
							if (messageLength < 10000 && messageLength > 0)
							{
								reading = true;
								rawMessage = new byte[messageLength+1];
								dis.readFully(rawMessage, 0, messageLength);
							}
					} catch (IOException e) { // If an I/O exception arrives here, this is not good.
							System.out.println("Reading incoming OSC message failed, retrying ...");
							System.out.println(e);
							try {
								Thread.sleep(2000); // 2 seconds before retrying should be enough
							}catch (InterruptedException e1) {
								System.err.println("Thread error ...");
								System.out.println(e1);
							}
					}
					
					// The raw message is fully received, try to read and return it
					if ((messageLength < 10000 && messageLength > 0) && rawMessage != null)
					{
						try {
							reader = new OscReader(rawMessage);
						} catch (OscMalformedBundleException | OscMalformedMessageException e1) {
							System.err.println("Malformed message received");
							System.out.println(e1);
							e1.printStackTrace();
						}
						try {
							if (reader != null)
								message = reader.getMessage();
						} catch (OscBadDataTypeRequestException e) {
							System.err.println("Message containing bad data types");
							System.out.println(e);
						}
						if (message != null)
						{
							messageReceived(message);
							messageLength = 0;
						}
					} // if
				}// while
			} // run()
		}; // Runnable
		Thread t = new Thread(loopReadSocket);
		t.start();
	}
	
	/**
	 * Notifies the observer that a new OSC message is ready to be processed.
	 * 
	 * @param message the newly received OSC message
	 */
	protected void messageReceived(OscMessage message) {
		if (observer != null)
			observer.messageReceived(message);
		else
			System.err.println("Message received but not sent because there is no valid observer.");
	}

	/**
	 * Connects to the M-START Server and sets up the activity.
	 * 
	 * @throws IOException 
	 */
	public void connect() throws IOException {
		System.out.print("Trying to connect to Server (" + IP + ":" + port +")...");
		socket = new Socket(IP, port);

		socketOutput = socket.getOutputStream();
		socketInput = socket.getInputStream();

		System.out.println("Connection to Server successfull ");
		OscMessageComposer msg = new OscMessageComposer(linkActivity);
		msg.pushString(activity);
		sendMessages(msg); // Send the activity link to finish the connection to server
		setConnected(true); // Set the connection status to connected 
		receive(); // Start the receive loop
	}

	/**
	 *  Returns true if this socket is connected.
	 *  
	 * @return boolean true if this socket is connected
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Sets the connected status of this socket.
	 *  
	 * @param connected the status of this socket.
	 */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
}
