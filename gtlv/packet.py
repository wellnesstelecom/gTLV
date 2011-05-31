# packet.py
#
# Copyright 2009 Wellness Telecom S.L.
#
# Author: Daniel Carrion (dcarrion@wtelecom.es)


"""
gTLV packet
"""

__docformat__   = "epytext en"

import struct

class HeaderFormat():
    """Representation of gTLV packet header
    
    @ivar application: Format of application field. As of gTLV v1, 2 bytes.
    @type application: String that corresponds to struct definition of the format.
    @ivar code: Format of code field. As of gTLV v1, 1 byte.
    @type code: String that corresponds to struct definition of the format.
    @ivar length: Format of packet length field. As of gTLV v1, 2 bytes.
    @type length: String that corresponds to struct definition of the format.
    """
    application = "H"
    code = "B"
    length = "H"

class AttributeFormat():
    """Representation of gTLV packet attributes
    
    @ivar type: Format of type field. As of gTLV v1, 1 byte.
    @type type: String that corresponds to struct definition of the format.
    @ivar length: Format of attribute length field. As of gTLV v1, 1 byte.
    @type length: String that corresponds to struct definition of the format.
    @ivar value: Format of value field. Not used as it depends on attribute type.
    @type value: Empty string.
    """
    type = "B"
    length = "H"
    value = ""

class ClassList():
    """ClassList keeps a record of available classes to be used by the encoding/decoding engines.
    """
    def __init__(self, items):
        """ Constructor. Adds first items to the internal list.
        
        @param items: Initial list of available classes.
        @type items: Tuple of classes.
        """
        self.items = []
        self.append(items)
        
    def append(self, items):
        """ Adds items to the internal list.
        
        @param items: List of available classes to be added.
        @type items: Tuple of classes.
        """
        for item in items:
            if not item in self.items:
                self.items.append(item)
    
    def find(self, needle):
        """ Finds a given class in the internal list. Must be overridden by subclasses.
        
        @param needle: Class to be found.
        @type needle: Class.
        """
        pass

class AttributeList(ClassList):
    """Keeps a record of available attribute classes to be used by the encoding/decoding engines.
    """
    def find(self, needle):
        """ Finds a given attribute type in the list of available attribute classes.
        
        @param needle: Attribute type to be found.
        @type needle: Integer.
        @return: Attribute class found.
        @rtype: Attribute class or None.
        """
        for item in self.items:
            if item.type == needle:
                return item
        return None

class PacketList(ClassList):
    """Keeps a record of available packet classes to be used by the encoding/decoding engines.
    """
    def find(self, needle):
        """ Finds a given packet type in the list of available packet classes, based on application
        and code fields..
        
        @param needle: Packet application and code combination to be found in the list.
        @type needle: Tuple of two integers.
        @return: Packet class found.
        @rtype: Packet class or None.
        """
        for item in self.items:
            if item.application == needle[0] and item.code == needle[1]:
                return item
        return None

class Attribute:
    """Generic placeholder for TLV attributes. Subclasses must fill C{type}, C{typedef} and C{fields} on
    definition and C{value} on instantiation.
       
    @ivar type: Attribute type.
    @type type: Integer.
    @ivar value: Attribute value.
    @type value: Depends on the attribute type.
    @ivar typedef: Value data type that corresponds to the attribute type.
    @type typedef: String. As of gTLV v1, one of 'integer', 'boolean', 'timestamp', 'string' or 'octets'.
    @ivar fields: Value format for 'octets' attributes.
    @type fields: Tuple. As of gTLV v1, one of 'integer', 'boolean' or 'timestamp'.
    """
    type = None
    value = None
    typedef = None
    fields = None

    def __init__(self):
        """ Constructor. Does nothing for now. Subclasses must call this constructor from their constructors.
        """
        pass
        
    def encode(self):
        """ Converts attribute data into a raw string that can be inserted in a raw packet.
        
        @return: Raw attribute.
        @rtype: String or None.
        """
        if self.typedef == 'integer':
            format = '!' + AttributeFormat.type + AttributeFormat.length + 'i'
            return struct.pack(format, self.type, struct.calcsize(format), self.value)
        if self.typedef == 'timestamp':
            format = '!' + AttributeFormat.type + AttributeFormat.length + 'I'
            return struct.pack(format, self.type, struct.calcsize(format), self.value)
        if self.typedef == 'boolean':
            format = '!' + AttributeFormat.type + AttributeFormat.length + 'B'
            return struct.pack(format, self.type, struct.calcsize(format), self.value)
        if self.typedef == 'string':
            format = '!' + AttributeFormat.type + AttributeFormat.length + str(len(self.value)) + 's'
            return struct.pack(format, self.type, struct.calcsize(format), self.value)
        if self.typedef == 'octets':
            values = ""
            i = 0
            for field in self.fields:
                if field == 'integer':
                    values += struct.pack('!i', self.value[i])
                if field == 'boolean':
                    values += struct.pack('!B', self.value[i])
                if field == 'timestamp':
                    values += struct.pack('!I', self.value[i])
                if field == 'string':
                    format = '!' + str(len(self.value[i])) + 's'
                    values += struct.pack('!i', struct.calcsize(format))
                    values += struct.pack(format, self.value[i])
                i += 1
            attribute_header_format = '!' + AttributeFormat.type + AttributeFormat.length
            attribute_length = struct.calcsize(attribute_header_format) + len(values)
            raw_attribute = struct.pack(attribute_header_format, self.type, attribute_length) + values
            return raw_attribute
        return None

class Packet:
    """Generic placeholder for TLV packets. Subclasses must fill C{application}, C{code},
    C{mandatory_attributes} and C{optional_attributes} (if any) on definition.
    
    @ivar application: Application identifier.
    @type application: Integer.
    @ivar code: Packet code.
    @type code: Integer.
    @ivar mandatory_attributes: List of mandatory attributes.
    @type mandatory_attributes: Dictionary. Made up of C{attribute_class: multiplicity} items.
    @ivar optional_attributes: List of optional attributes.
    @type optional_attributes: Dictionary. Made up of C{attribute_class: multiplicity} items.
    
    Multiplicity means maximum number of occurrences, like C{1}, C{23} or C{float('inf')}.
    """
    application = None
    code = None
    mandatory_attributes = None
    optional_attributes = None

    def __init__(self):
        """ Constructor. Subclasses must call this constructor from their constructors.
        """
        self.length = None
        self.actual_attributes = []

    def count(self, attribute_class):
        """ Count how many attributes of a given class are currently in the packet.
        
        @param attribute_class: Attribute class for the search.
        @type attribute_class: Attribute class.
        @return: Number of occurrences.
        @rtype: Integer.
        """
        matches = 0
        for attribute in self.actual_attributes:
            if attribute.__class__ == attribute_class:
                matches += 1
        return matches
        
    def get_values(self, attribute_class):
        """ Get all the values of all the attributes of a given class found in the packet.
        
        @param attribute_class: Attribute class for the search.
        @type attribute_class: Attribute class.
        @return: List of corresponding values.
        @rtype: List.
        """
        values = []
        for attribute in self.actual_attributes:
            if attribute_class == attribute.__class__:
                values.append(attribute.value)
        return values
        
    def add_attribute(self, attribute):
        """ Add an attribute to the packet.
        
        @param attribute: Attribute to be added.
        @type attribute: Attribute instance.
        @return: C{True} on success, C{False} on failure.
        @rtype: C{True} or C{False}.
        """
        try:
            multiplicity = self.mandatory_attributes[attribute.__class__]
            if self.count(attribute.__class__) < multiplicity:
                self.actual_attributes.append(attribute)
                return True
            else:
                return False
        except:
            try:
                multiplicity = self.optional_attributes[attribute.__class__]
                if self.count(attribute.__class__) < multiplicity:
                    self.actual_attributes.append(attribute)
                    return True
                else:
                    return False
            except:
                return False
        return False

    def encode(self):
        """ Converts packet data into a raw string that can be sent over a socket.
        
        @return: Raw packet.
        @rtype: String or None.
        
        # I{TODO: Check that all mandatory attributes are present}
        """
        raw_attributes = ""
        for attribute in self.actual_attributes:
            raw_attributes += attribute.encode()
        format = '!' + HeaderFormat.application + HeaderFormat.code + HeaderFormat.length + str(len(raw_attributes)) + 's'
        header_length = struct.calcsize('!' + HeaderFormat.application + HeaderFormat.code + HeaderFormat.length)
        return struct.pack(format, self.application, self.code, header_length + len(raw_attributes), raw_attributes)

def decode(raw_packet, list_manager):
    """ Converts raw packet data from a socket into a Packet instance.
        
    @param raw_packet: Attribute to be added.
    @type raw_packet: Attribute instance.
    @param list_manager: List of available packets and attribute classes.
    @type list_manager: Class that has a packet_list attribute of class PacketList and an attribute_list attribute of class AttributeList.
    @return: Packet instance that corresponds to raw packet data.
    @rtype: Packet or None.
    """
    attributes = []
    (application, code, length) = struct.unpack_from('!' + HeaderFormat.application + HeaderFormat.code + HeaderFormat.length, raw_packet)
    if length != len(raw_packet):
        return None

    packet_class = list_manager.packet_list.find((application, code))
    if packet_class:
        packet = packet_class()
    else:
        return None
    
    raw_attributes_to_process = raw_packet[struct.calcsize('!' + HeaderFormat.application + HeaderFormat.code + HeaderFormat.length):]
    while len(raw_attributes_to_process) > struct.calcsize('!' + AttributeFormat.type + AttributeFormat.length):
        (type, length) = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length, raw_attributes_to_process)
        attribute_class = list_manager.attribute_list.find(type)
        if attribute_class:
            if attribute_class.typedef == 'integer':
                (type, length, value) = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length + 'i', raw_attributes_to_process)
            if attribute_class.typedef == 'boolean':
                (type, length, value) = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length + 'B', raw_attributes_to_process)
            if attribute_class.typedef == 'timestamp':
                (type, length, value) = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length + 'I', raw_attributes_to_process)
            if attribute_class.typedef == 'string':
                (type, length, value) = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length + str(length - struct.calcsize('!' + AttributeFormat.type + AttributeFormat.length)) + 's', raw_attributes_to_process)
            if attribute_class.typedef == 'octets':
                value = []
                format = ""
                index = struct.calcsize('!' + AttributeFormat.type + AttributeFormat.length)
                for field in attribute_class.fields:
                    if field == 'integer':
                        format = '!i'
                        val = struct.unpack_from(format, raw_attributes_to_process[index:])
                        
                    if field == 'boolean':
                        format = '!B'
                        val = struct.unpack_from(format, raw_attributes_to_process[index:])
                        
                    if field == 'timestamp':
                        format = '!I'
                        val = struct.unpack_from(format, raw_attributes_to_process[index:])
                        
                    if field == 'string':
                        """ First we have the length of the string """
                        strlen = struct.unpack_from('!i', raw_attributes_to_process[index:])
                        index += struct.calcsize('!i');
                        format = '!' + str(strlen[0]) + 's'
                        val = struct.unpack_from(format, raw_attributes_to_process[index:])
                    index += struct.calcsize(format)
                    value.append(val)
                #tlv = []
                #tlv = struct.unpack_from('!' + AttributeFormat.type + AttributeFormat.length + format, raw_attributes_to_process)
                #value = tlv[struct.calcsize('!' + AttributeFormat.type + AttributeFormat.length):]
            raw_attributes_to_process = raw_attributes_to_process[length:]
            attribute = attribute_class(value)
            packet.add_attribute(attribute)
    return packet
