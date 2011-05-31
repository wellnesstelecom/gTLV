package com.wsn.gtlv.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * 
 * Copyright 2009 Wellness Telecom S.L.
 * 
 * @author Daniel Carrion (dcarrion@wtelecom.es)
 * @author Felix Lopez (flopez@wtelecom.es)
 * 
 * This class represents the client in the common "client/server". It's very easy to use:
 * 
 *  Target target = new Target("192.168.1.34", 5400);
 *		CommandRequest new_packet = new CommandRequest();      
 *	    MoteId moteId = new MoteId();
 *	    moteId.setValue(-1);
 *	    new_packet.addAttribute(moteId);
 *	    ActionType type = new ActionType();
 *	    type.setValue(3);
 *		new_packet.addAttribute(type);
 *		TimeBetweenSamples samples = new TimeBetweenSamples();
 *		samples.setValue(miliseconds);
 *		new_packet.addAttribute(samples);
 *		byte success = InfoMoteActivity.INTERVAL_FAIL;
 *        try {
 *        	CommandAcknowledgement rsp = (CommandAcknowledgement)target.send(new_packet);
 *			if (rsp != null) {
 *				if (rsp.wasSuccessful()) {
 *					success = InfoMoteActivity.INTERVAL_SUCCESS;			
 *				}
 *			}
 *		} catch (Exception e) {
 *			e.printStackTrace();
 *		} finally {
 *			sensorHandler.sendEmptyMessage(success);
 *		}
 */
public class Target implements Runnable {

	final Object locker = new Object();	
	private final String address;
	private final int port;
	private Packet packetRequest = null;
	private Packet packetResponse = null;
	private IOException exception = null;
	private ArrayList<Packet> packets = null;
	private ArrayList<Attribute> attributes = null;
	
	/**
	 * @param address
	 * @param port
	 */
	public Target(String address, int port, ArrayList<Packet> packets, ArrayList<Attribute> attributes) {
		this.address = address;
		this.port = port;
		this.attributes = attributes;
		this.packets = packets;
	}

	/**
	 * It sends the packet to the server and waits until a response is received.
	 * @param packet
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public Packet send(Packet packet) throws IOException, InterruptedException {
		this.packetRequest = packet;
		   synchronized(locker)
           {
			   new Thread(this).start();
			   locker.wait();
           }
		   if (packetResponse == null) {
			   throw exception;
		   }
		return packetResponse;
	}

	public void run() {
		try {
			InetAddress serverAddr = InetAddress.getByName(address); 
			Socket socket = new Socket(serverAddr, port);
			OutputStream out = socket.getOutputStream();
			out.write(packetRequest.encode());
			int read = 0;
			InputStream in = socket.getInputStream();
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			byte[] buffer = new byte[256];
			while ((read = in.read(buffer)) != -1) {
				byteOut.write(buffer);
			}
			socket.close();
			packetResponse = Packet.decode(byteOut.toByteArray(), packets, attributes);
		} catch (IOException e) {
			exception = e;
		}
		synchronized(locker)
		{
			locker.notify();
		}

	}
}
