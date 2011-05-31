package com.wsn.gtlv.network;



import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Copyright 2009 Wellness Telecom S.L.
 * 
 * @author Daniel Carrion (dcarrion@wtelecom.es)
 * @author Felix Lopez (flopez@wtelecom.es)
 *
 * 
 * Now each attribute knows hot encode itself, but not how to decode, in the future each attribute must decode itself.
 * 
 * This is the way to get the return values:
 * 
 * Object value = getValue();
 * 
 * But the normal way is to use the Packet:
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
public class Attribute {
	private byte typeAttribute = 0;
	protected final Class typeValue;
	protected List<Class> fields = new ArrayList<Class>();
	private Object value = null;
	
	private int length = 0;
	
	/**
	 * 
	 * @param type
	 * @param typedef
	 */
	public Attribute(byte typeAttribute, Class typeValue) {
		this.typeAttribute = typeAttribute;
		this.typeValue = typeValue;
	}
	/**
	 * It returns the length.
	 * @return
	 */
	public int encodedLength() {
		return length;
	}
	

	/**
	 * It encodes the attribute.
	 * @return the encoded bytes. 
	 * @throws IOException 
	 */
	public byte[] encode() throws IOException {
		byte[] ret = null;
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(byteOut);
		try {
			//Int = 4
			// short = 2
			// byte = 1
			data.writeByte(typeAttribute);
			if (typeValue.equals(Integer.class)) {
				data.writeShort(7);
				data.writeInt((Integer)value);
				length = 7;
			} else if (typeValue.equals(Timestamp.class)) {
				data.writeShort(7);
				data.writeInt(((Long)value).intValue()); // OJO es Unsigned int, para java será un long
				length = 7;
			} else if (typeValue.equals(Boolean.class)) {
				data.writeShort(2);
				data.writeBoolean((Boolean)value);
				length = 2;
			} else if (typeValue.equals(String.class)) {
				String s = (String) value;
				length = s.length() + 3; // type(byte) + length (short)
				data.writeShort(length);
				data.writeBytes(s);
				// ese 2 para que es?¿¿?? return (struct.pack('!BB' + str(len(self.value)) + 's', self.type, 2 + len(self.value), self.value), 2 + len(self.value))
			} else if (typeValue.equals(OctectsType.class)) {
				ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
				DataOutputStream dataTemp = new DataOutputStream(tempOut);
				ArrayList<Object> values = (ArrayList<Object>)value;
				length = 0;
				///length += 1; // type is 1 byte
				int i = 0;
				for (Class field: fields) {
					if (field.equals(Integer.class)) {
						//data_types += 'i';
						dataTemp.writeInt((Integer)values.get(i));
						length += 4;
					}
					if (field.equals(Timestamp.class)) {
						//data.writeShort(7);
						dataTemp.writeInt(((Long)values.get(i)).intValue()); // OJO es Unsigned int, para java será un long
						length += 4;
					}
					if (field.equals(Boolean.class)) { 
						//data_types += 'B';
						dataTemp.writeBoolean((Boolean)values.get(i));
						length += 1;
					}
					if (field.equals(String.class)) {
						
						String value = (String)values.get(i);
						length += value.length()+ 2 ; //short is 2 bytes
						dataTemp.writeShort(value.length());
						dataTemp.writeBytes(value);
					}
					i++;
				}
				
				data.writeShort(length);
				length += 3; // length is short = 2 + type = 1 byte
				
				data.write(tempOut.toByteArray());
				dataTemp.close();
				tempOut.close();
			}
			ret = byteOut.toByteArray();
		} finally {
			if (data != null) {
				data.close();
			}
			if (byteOut != null) {
				byteOut.close();
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param packet
	 * @return
	 */
	public boolean equals(Attribute att) {
		return isEquals(att.typeAttribute); 
	}
	
	/**
	 * It returns weather it's equal or not
	 * @param packet
	 * @return
	 */
	public boolean isEquals(int type) {
		return this.typeAttribute == type; 
	}

	public Object getValue() {
		return value;
	}
	
	/**
	 * Each attribute must convert the object into the concrete type.
	 * @param value
	 */
	public void setValue(Object value) {
		this.value = value;
	}

}
            
/*
class Attribute:
    type = None
    typedef = None
    fields = None
    value = None

    # Typedefs (as described in gTLV v1): i: integer, t: timestamp, b: Boolean, s: string, o: octets
    # Fields: list of typedefs
    def __init__(self):
        pass
        
    def encode(self):
        if self.typedef == 'i':
            return (struct.pack('!BBi', self.type, 6, self.value), 6)
        if self.typedef == 't':
            return (struct.pack('!BBI', self.type, 6, self.value), 6)
        if self.typedef == 'b':
            return (struct.pack('!BBB', self.type, 3, self.value), 3)
        if self.typedef == 's':
            return (struct.pack('!BB' + str(len(self.value)) + 's', self.type, 2 + len(self.value), self.value), 2 + len(self.value))
        if self.typedef == 'o':
            data_types = ""
            value_length = 0
            for field in self.fields:
                if field == 'i':
                    data_types += 'i'
                    value_length += 4
                if field == 'b':
                    data_types += 'B'
                    value_length += 1
                if field == 't':
                    data_types += 'I'
                    value_length += 4
            i = 0
            bytes = struct.pack('!BB', self.type, value_length + 2)
            for data_type in data_types:
                bytes += struct.pack('!' + data_type, self.value[i])
                i += 1
            return (bytes, value_length + 2)
        return False
*/