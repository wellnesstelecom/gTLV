#!/usr/bin/python
#
# example.py
#
# Copyright 2009 Wellness Telecom S.L.
#
# Author: Daniel Carrion (dcarrion@wtelecom.es)

import struct
from gtlv.packet import Attribute, AttributeList, Packet, PacketList, decode
from gtlv.network import GtlvServer, Target

# Application
SAMPLE_APPLICATION = 0x0000

# Attribute definitions
class MoteId(Attribute):
    type = 0x01
    typedef = 'integer'
    
    def __init__(self, value):
        Attribute.__init__(self)
        self.value = value

class MagnitudeId(Attribute):
    type = 0x03
    typedef = 'integer'
    
    def __init__(self, value):
        Attribute.__init__(self)
        self.value = value

class Sample(Attribute):
    type = 0x04
    typedef = 'octets'
    fields = ('integer', 'integer', 'integer', 'integer')
    
    def __init__(self, (mote_id, magnitude, value, timestamp)):
        Attribute.__init__(self)
        self.value = (mote_id, magnitude, value, timestamp)

# Packet definitions
class DataRequest(Packet):
    application = SAMPLE_APPLICATION
    code = 0x01
    mandatory_attributes = {MoteId: float('inf'), MagnitudeId: 1}

    def __init__(self):
        Packet.__init__(self)

class DataResponse(Packet):
    application = SAMPLE_APPLICATION
    code = 0x02
    mandatory_attributes = {Sample: float('inf')}

    def __init__(self):
        Packet.__init__(self)

# List of allowed packet and attribute classes
class ListManager():        
    packet_list = PacketList((DataRequest, DataResponse))
    attribute_list = AttributeList((MoteId, MagnitudeId, Sample))

# Callback function that will be called after a packet is received
class Server(GtlvServer):
    def callback(self, raw_packet):
        packet = decode(raw_packet, self.list_manager)
        new_packet = DataResponse()
        new_packet.add_attribute(Sample((1, 2, 3, 4)))
        new_packet.add_attribute(Sample((5, 6, 7, 8)))
        return new_packet.encode()
    
server = Server("localhost", 50000, ListManager)
server.start()

# In this example, a DataRequest packet is sent with two mote-ids and one magnitude-id
# The response is a DataResponse packet with a two sample attributes.
new_packet = DataRequest()
new_packet.add_attribute(MoteId(123))
new_packet.add_attribute(MoteId(234))
new_packet.add_attribute(MagnitudeId(1))
print "Sent:"
print new_packet.get_values(MoteId)
print new_packet.get_values(MagnitudeId)

target = Target("localhost", 50000, ListManager)
rsp = target.send(new_packet)
print "Received:"
print rsp.get_values(Sample)
