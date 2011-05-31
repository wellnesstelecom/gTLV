# network.py
#
# Copyright 2009 Wellness Telecom S.L.
#
# Author: Daniel Carrion (dcarrion@wtelecom.es)


"""
gTLV networking
"""

__docformat__   = "epytext en"

import socket, threading, SocketServer
from packet import Packet, decode

MAX_REQUEST_SIZE = 2048
MAX_RESPONSE_SIZE = 1024

class Target():
    """ Represents a client that targets a server and that can be used to send
        packets to that server.
    """
    def __init__(self, target_address, target_port, list_manager):
        """ Constructor. It only saves parameters to internal attributes.

            @param target_address: IP address of FQDN for the server.
            @type target_address: String.
            @param target_port: TCP listening port for the server.
            @type target_port: Integer.
            @param list_manager: List of available packets and attribute classes.
            @type list_manager: Class that has a packet_list attribute of class PacketList and an attribute_list attribute of class AttributeList.
         """
        self.target_address = target_address
        self.target_port = target_port
        self.list_manager = list_manager

    def send(self, packet):
        """ Sends a given packet to the server and waits for a reply.

            @param packet: Packet to be sent.
            @type packet: Packet instance.
            @return: Packet return by the server..
            @rtype: Packet instance or None.
        """
        if not isinstance(packet, Packet):
            return None
        tmp_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tmp_socket.connect((self.target_address, self.target_port))
        tmp_socket.send(packet.encode())
        data = tmp_socket.recv(MAX_RESPONSE_SIZE)
        tmp_socket.close()
        return decode(data, self.list_manager)

class Handler(SocketServer.BaseRequestHandler):
    """ Request handler for the server.
    """
    def handle(self):
        """ Waits until data is received, calls server's callback function and sends the result
            of the callback to the original sender.
        """
        data = ""
        self.request.settimeout(1)
        try:
            while True:
                data += self.request.recv(MAX_REQUEST_SIZE)
        except:
            pass
        response = self.server.callback(data)
        self.request.send(response)
        
class GtlvServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    """ Represents a server that waits for incoming gTLV packets.
    """
    def __init__(self, ip, port, list_manager):
        """ Constructor. It only saves parameters to internal attributes and calls the
            constructor of its parent class.

            @param ip: Local IP address to bind to.
            @type ip: String.
            @param port: Local TCP port to bind to.
            @type port: Integer.
            @param list_manager: List of available packets and attribute classes.
            @type list_manager: Class that has a packet_list attribute of class PacketList and an attribute_list attribute of class AttributeList.
         """
        self.list_manager = list_manager
        SocketServer.TCPServer.__init__(self, (ip, port), Handler)

    def callback(self, raw_packet):
        """ Callback function to process received packets. Needs to be overridden by subclasses.

            @param raw_packet: Raw packet as received on the socket.
            @type raw_packet: String.
        """
        pass
        
    def start(self):
        """ Start the server in a new thread. Worker threads will be used to process incoming packets.
        """
        server_thread = threading.Thread(target = self.serve_forever)
        server_thread.daemon = True
        server_thread.start()
                                                    