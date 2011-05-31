/**
 * 
 */
package com.wsn.gtlv.network;

import java.net.Socket;

/**
 * Copyright 2009 Wellness Telecom S.L.
 * 
 * @author Daniel Carrion (dcarrion@wtelecom.es)
 * @author Felix Lopez (flopez@wtelecom.es)
 *
 * You have to extend this class in other to have your own server:
 * When a request is coming an new thread starts so you need to do your business in the run method: 
 * 
 * 	public void run() {
 * 		try {
 *			Socket client = getClient();
 *			InputStream in = client.getInputStream();
 *			try {
 *				byte[] buffer = new byte[1024];
 *				in.read(buffer);
 *				Packet pa = Packet.decode(buffer, Constants.packets, Constants.attributes);
 *				if (pa instanceof AlarmIndication) {
 *					//do something
 *				}
 *			} catch(Exception e) {
 *			} finally {
 *
 *				client.getOutputStream().close();
 *				in.close();
 *				client.close();
 *				client = null;
 *			}
 *		} catch (IOException e) {
 *			// TODO Auto-generated catch block
 *		}
 *	}
 *
 */
public abstract class ServerThread implements Runnable {

	private  Socket client;
	
	/**
	 * @return the client
	 */
	protected Socket getClient() {
		return client;
	}

	/**
	 * @param client the client to set
	 */
	protected void setClient(Socket client) {
		this.client = client;
	}
}
