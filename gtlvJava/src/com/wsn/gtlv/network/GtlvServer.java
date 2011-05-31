/**
 * 
 */
package com.wsn.gtlv.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Copyright 2009 Wellness Telecom S.L.
 * 
 * @author Daniel Carrion (dcarrion@wtelecom.es)
 * @author Felix Lopez (flopez@wtelecom.es)
 *
 *
 */
public class GtlvServer implements Runnable {

	private static boolean STARTED = false;
	private ServerThread serverThread = null;
	private final int port;
	private final InetAddress inetAddress;
	
	public GtlvServer(ServerThread serverThread, int port, InetAddress inetAddress) {
		this.serverThread = serverThread;
		this.port = port;
		this.inetAddress = inetAddress;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ServerSocket serverSocket = null;
		try {
			STARTED = true;
			serverSocket = new ServerSocket(port, 0, inetAddress);
			while (true) {   
				serverThread.setClient(serverSocket.accept());
				new Thread(serverThread).start();
			}
		} catch (IOException e1) {
			if (serverSocket != null) {
				try {
					STARTED = false;
					serverSocket.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean isStarted() {
		return STARTED;
	}
	
}
