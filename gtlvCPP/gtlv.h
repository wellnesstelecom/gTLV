/**
 * \file gtlv.h
 * \brief C++ GTLV implementation.
 *
 * Relaxed GTLV implementation for this middleware, where relax means compliance with specific applications
 * is not enforced by this library but must rather be taken care of by the user.
 *
 * Copyright 2010 Wellness Telecom S.L.
 *
 * \author Daniel Carrion (dcarrion@wtelecom.es)
 *
 * \section Example
 *
 * \subsection ex1 Listen for GTLV request and answer with same packet plus two new attributes.
\code
#include <comm.h>
#include <tcp.h>
#include <gtlv.h>
#include <transporter.h>

using namespace gtlv;

#define SAMPLE_APPLICATION 0x0000
#define DATA_REQUEST 0x01
#define DATA_RESPONSE 0x02
#define MOTE_ID 0x01
#define MAGNITUDE_ID 0x03
#define SAMPLE 0x04
#define RX_BUFFER_SIZE 500

typedef struct {
  uint32_t mote_id;
  uint32_t magnitude_id;
  uint32_t value;
  uint32_t timestamp;
} sample_subattribute;

int callback(byte *rx_buffer, size_t rx_buffer_size, byte *&tx_buffer, void *not_used)
{
  sample_subattribute sample_response = {10, 11, 12, 13};

  // Just sends back the received packet plus two extra attributes
  Packet received(rx_buffer, rx_buffer_size);
  received.add_attribute(MAGNITUDE_ID, (uint32_t) 0x05060708);
  received.add_attribute(SAMPLE, (uint8_t *) &sample_response, sizeof(sample_response));
  return received.encode(tx_buffer); // This packet is not PL-GTLV compliant, but hey it's just an example.
}

int main()
{
  byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED};
  byte ip[] = {192, 168, 136, 36};
  Server server(50000);

  init();

  Ethernet.begin(mac, ip);
  server.begin();
  Transporter <Server> porter(&server);

  while(1) {
    porter.handle_request(callback);
  }
}
\endcode
 */

#ifndef _GTLV_H_
#define _GTLV_H_

#include <stdint.h>
#include <tools.h>

namespace gtlv {
  /**
   * \brief Generic placeholder for TLV attributes.
   *
   * Attribute objects are used for attribute manipulation (get/set value, encode/decode...).
   */
  class Attribute
  {
    public:
      uint8_t type; ///< GTLV attribute type field
      uint16_t length; ///< GTLV attribute length field
      uint8_t *value; ///< GTLV attribute value field

      Attribute();
      Attribute(uint8_t type, uint8_t value);
      Attribute(uint8_t type, uint32_t value);
      Attribute(uint8_t type, uint8_t *value, uint16_t value_length);
      ~Attribute();
      Attribute &operator= (Attribute &rhs);
      void get_value(uint8_t *&value, uint16_t &length);
      void set_value(uint8_t *bytes, uint16_t value_length);
      uint16_t get_value_length();
      uint8_t *encode();
  };

  /**
   * \brief Generic placeholder for TLV packets.
   *
   * Packet objects are used for packet manipulation (add/get attributes, encode/decode...).
   */
  class Packet
  {
    public:
      uint16_t application; ///< GTLV packet application field
      uint8_t code; ///< GTLV packet code field
      uint16_t length; ///< GTLV packet length field
      List <Attribute> actual_attributes; ///< List of attributes in packet

      Packet(uint8_t *raw_packet, uint16_t length);
      Packet(uint16_t application = 0, uint8_t code = 0);
      void add_attribute(uint8_t type, uint8_t value);
      void add_attribute(uint8_t type, uint32_t value);
      void add_attribute(uint8_t type, uint8_t *value, uint16_t value_length);
      List <Attribute> *get_attributes();
      uint16_t encode(uint8_t *&raw_packet);
      void decode(uint8_t *raw_packet, size_t bytes_to_read);
  };
}

#endif // _GTLV_H_
