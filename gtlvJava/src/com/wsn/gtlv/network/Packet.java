package com.wsn.gtlv.network;




import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Copyright 2009 Wellness Telecom S.L.
 * 
 * @author Daniel Carrion (dcarrion@wtelecom.es)
 * @author Felix Lopez (flopez@wtelecom.es)
 *
 * 
 * The packet knows how to encode itself, how to decode and how to decode the attributes, in the future each attribute must decode itself.
 * 
 * This is the way to get the return values:
 * 
 * Object[] values = packet.getValues(new AttributeToRetrieve());
 * 
 * The concrete Packet know how to get the value of its attributes, for instance:
 * CommandAcknowledgement has the method: wasSuccessful() which tells you if the operation was right. Internally this method has:
 * 
 *	public boolean wasSuccessful() {
 *		Object[] values = getValues(new Success());
 *		return (Boolean)values[0];
 *	}
 * There may be some Packet which don't have this kind of methods yet, as we work with the framework we're going to add them. 
 *  
 */
public class Packet {
	
	private static final int ATTRIBUTE_LENGTH_LENGTH = 2;
	private static final int ATTRIBUTE_TYPE_LENGTH = 1;
	private static final int HEADER = 5;
	private int application = 5;
	private byte code = 3; 
	private HashMap<Class, Integer> mandatory_attributes = new HashMap<Class, Integer>();
	private HashMap<Class, Integer> optional_attributes = new HashMap<Class, Integer>();
	private ArrayList<Attribute> actual_attributes = new ArrayList<Attribute>();
	int length;
	
	/**
	 * 
	 * @param application
	 * @param code
	 */
	public Packet(int application, byte code) {
		this.application = application;
		this.code = code;
	}

	/**
	 * 
	 * @param theClass
	 * @param i
	 */
	public void addMandatory_attributes(Class theClass, Integer i) {
		mandatory_attributes.put(theClass, i);
	}


	public void addOptional_attributes(Class theClass, Integer i) {
		optional_attributes.put(theClass, i);
	}

	/**
	 * It return the number of the same attribute.
	 * @param attribute
	 * @return
	 */
	public int count(Attribute attribute) {
		int mathes = 0;
		for (Attribute att : actual_attributes) {
			if (att.getClass().isInstance(attribute)) {
				mathes++;
			}
		}
		return mathes;
	}
	
	/**
	 * Gets the values for that attribute
	 * @param attribute
	 * @return
	 */
	public Object[] getValues(Attribute attribute) {
		ArrayList<Object> values = new ArrayList<Object>();
		for (Attribute att : actual_attributes) {
			if (att.getClass().isInstance(attribute)) {
				values.add(att.getValue());
			}
		}
		return values.toArray();
	}
	
	/**
	 * Gets the attributes with the same type
	 * @param attribute
	 * @return
	 */
	public ArrayList<Attribute> getAttributes(Attribute attribute) {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (Attribute att : actual_attributes) {
			if (att.getClass().isInstance(attribute)) {
				attributes.add(att);
			}
		}
		return attributes;
	}
	
	/**
	 * It returns weather the attribute is added right or not
	 * @param attribute
	 * @return
	 */
	public boolean addAttribute(Attribute attribute) {
		boolean ret = false;
		Integer multiplicity = mandatory_attributes.get(attribute.getClass());
		if (multiplicity == null) {
			multiplicity = optional_attributes.get(attribute.getClass());
		}
		if (multiplicity != null) {
			  if (count(attribute) < multiplicity) {
				  actual_attributes.add(attribute);
			  }
			ret = true;
		}
		return ret;
	}

	/**
	 * 
	 * @param packet
	 * @return
	 */
	public boolean equals(Packet packet) {
		return isEquals(packet.application, packet.code); 
	}
	
	/**
	 * It returns weather it's equal or not
	 * @param packet
	 * @return
	 */
	public boolean isEquals(int application, byte code) {
		return this.application == application && this.code == code; 
	}
	
	
	public byte[] encode() throws IOException {
		byte[] ret = null;
		ByteArrayOutputStream byteOutAtt = new ByteArrayOutputStream();
		
		ByteArrayOutputStream byteFinal = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(byteFinal);
		try {
			for (Attribute att: actual_attributes) {
				byteOutAtt.write(att.encode());
				length += att.encodedLength();
			}
			
			data.writeShort(application);
			data.writeByte(code);
			data.writeShort(5 + length);
			
			data.write(byteOutAtt.toByteArray());
			ret = byteFinal.toByteArray();
		} finally {
			if (byteFinal != null) {
				byteFinal.close();
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param raw
	 * @param packets
	 * @param attributes
	 * @return
	 * @throws IOException
	 */
	public static Packet decode(byte[] raw, ArrayList<Packet> packets, ArrayList<Attribute> attributes ) throws IOException {
		Packet pack = null;
		ByteArrayInputStream byteIn = new ByteArrayInputStream(raw);
		DataInputStream data = new DataInputStream(byteIn);
		int bytes = 0;
		int application = data.readShort();
		bytes += 2;
		byte code = data.readByte();
		bytes += 1;
		int length = data.readShort();
		bytes += 2;
		//length -= 5;
		try {	
			pack = findPacked(packets, application, code);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		if (pack == null) {
			throw new IOException("Packet not found for the application: " + application + " and code: " + code);
		}
		
		while (bytes < length) {
			byte type;
			short lengthAtt;
			type = data.readByte();
			bytes += ATTRIBUTE_TYPE_LENGTH;
			lengthAtt = data.readShort();
			bytes += ATTRIBUTE_LENGTH_LENGTH;
			Attribute att = null;
			try {
				att = findAttribute(attributes, type);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
			
			if (att != null) {
				if (att.typeValue.equals(Integer.class)) {
					att.setValue(new Integer(data.readInt()));
					bytes += 4;
				} else if (att.typeValue.equals(Timestamp.class)) {
					att.setValue(new Long(data.readInt()));
					bytes += 4;	
				} else if (att.typeValue.equals(Boolean.class)) {
					att.setValue(new Boolean(data.readBoolean()));
					bytes += 1;	
				}  else if (att.typeValue.equals(String.class)) {
					int strLen = lengthAtt - ATTRIBUTE_LENGTH_LENGTH - ATTRIBUTE_TYPE_LENGTH;
					byte[] buffer = new byte[strLen];
					data.read(buffer);
					att.setValue(new String(buffer));
					bytes += strLen;	
				} else if (att.typeValue.equals(OctectsType.class)) {
					ArrayList<Object> values = new ArrayList<Object>();
					att.setValue(values);
					for (Class field: att.fields) {
						if (field.equals(Integer.class)) {
							values.add(new Integer(data.readInt()));
							bytes += 4;
						}
						if (field.equals(Boolean.class)) { 
							values.add(new Boolean(data.readBoolean()));
							bytes += 1;
						}
						if (field.equals(Timestamp.class)) {
							values.add(new Long(data.readInt()));
							bytes += 4;
						}
						if (field.equals(String.class)) {
							int len = data.readInt();
							byte[] buffer = new byte[len];
							data.read(buffer);
							String str = new String(buffer);
							values.add(str);
							bytes += str.length() + 4; // this 4 is because of the len	
						}
					}
				}
				pack.addAttribute(att);
			}
			
		}
		return pack;
	}

	/**
	 * 
	 * @param attributes
	 * @param type
	 * @return
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 */
	public static Attribute findAttribute(ArrayList<Attribute> attributes, byte type) throws IllegalAccessException, InstantiationException {
		Attribute att = null;
		for (Attribute theAtt : attributes) {
			if (theAtt.isEquals(type)) {
				att = theAtt.getClass().newInstance();
				break;
			}
		}
		return att;
	}

	/**
	 * 
	 * @param packets
	 * @param application
	 * @param code
	 * @return
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 */
	public static Packet findPacked(ArrayList<Packet> packets, 
			int application, byte code) throws IllegalAccessException, InstantiationException {
		Packet pack = null;
		for (Packet thePacket : packets) {
			if (thePacket.isEquals(application, code)) {
				pack = thePacket.getClass().newInstance();
				break;
			}
		}
		return pack;
	}
}
//
//def decode(raw_packet, list_manager):
//    attributes = []
//    (application, code, length) = struct.unpack_from("!HBH", raw_packet) #decodificas el string con todos los attributos, la app, el code
//    if length != len(raw_packet):
//        return None
//
//    packet_class = list_manager.packet_list.find((application, code)) # sacas el tipo de paquete 
//    if packet_class:
//        packet = packet_class()
//    else:
//        return None
//    
//    raw_attributes_to_process = raw_packet[5:] 
//    while len(raw_attributes_to_process) > 2: # no entiendo esto, recorres hasta que desempaquetas todos los atttributos pero no entiendo de donde viene el 2
//        (type, length) = struct.unpack_from("!BB", raw_attributes_to_process) # desempaquetas el attributo, solo los 2 primeros campos para poder saber cual es el tercero en función del tipo?
//        attribute_class = list_manager.attribute_list.find(type) # sacas el atributo
//        if attribute_class:
//            if attribute_class.typedef == 'i':
//                (type, length, value) = struct.unpack_from('!BBi', raw_attributes_to_process)
//            if attribute_class.typedef == 'b':
//                (type, length, value) = struct.unpack_from('!BBB', raw_attributes_to_process)
//            if attribute_class.typedef == 't':
//                (type, length, value) = struct.unpack_from('!BBI', raw_attributes_to_process)
//            if attribute_class.typedef == 's':
//                (type, length, value) = struct.unpack_from('!BB' + str(length - 2) + 's', raw_attributes_to_process)
//            if attribute_class.typedef == 'o':
//                data_types = ""
//                for field in attribute_class.fields:
//                    if field == 'i':
//                        data_types += 'i'
//                    if field == 'b':
//                        data_types += 'B'
//                    if field == 't':
//                        data_types += 'I'
//                tlv = []
//                tlv = struct.unpack_from('!BB' + data_types, raw_attributes_to_process)
//                value = tlv[2:]
//                length = tlv[1]
//            raw_attributes_to_process = raw_attributes_to_process[length:]
//            attribute = attribute_class(value)
//            packet.add_attribute(attribute)
//    return packet