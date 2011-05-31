/**
 * \file gtlv.cpp
 * \brief GTLV implementation for Arduino.
 * \version 0.1
 *
 * Relaxed GTLV implementation for Arduino, where relax means compliance with specific applications
 * is not enforced by this library but must rather be taken care of by the user.
 *
 * Copyright 2010 Wellness Telecom S.L.
 *
 * \author Daniel Carrion (dcarrion@wtelecom.es)
 */

#include <stdlib.h>
#include <string.h>

#include <gtlv.h>
#include <comm.h>
#include <tools.h>

using namespace gtlv;

/**
 * \brief Creates empty attribute object.
 *
 * Creates an empty attribute object, with no type, length or value.
 */
Attribute::Attribute(): type(0), length(0), value(NULL)
{
}
   
/**
 * \brief Creates attribute object for boolean values.
 * \param type Attribute type.
 * \param value Attribute value.
 *
 * Creates attribute object based on separate type and value arguments.
 */
Attribute::Attribute(uint8_t type, uint8_t value): type(type), length(0), value(NULL)
{
  Attribute::set_value(&value, sizeof(value));
}
   
/**
 * \brief Creates attribute object for integer/timestamp values.
 * \param type Attribute type.
 * \param value Attribute value.
 *
 * Creates attribute object based on separate type and value arguments.
 */
Attribute::Attribute(uint8_t type, uint32_t value): type(type), length(0), value(NULL)
{
  uint32_t value_network = htonl(value);

  Attribute::set_value((uint8_t *) &value_network, sizeof(value_network));
}

/**
 * \brief Creates attribute object for string/octet values.
 * \param type Attribute type.
 * \param value Attribute value.
 * \param value_length Size of value field.
 *
 * Creates attribute object based on separate type, value and value size arguments.
 */
Attribute::Attribute(uint8_t type, uint8_t *value, uint16_t value_length): type(type), length(0), value(NULL)
{
  Attribute::set_value(value, value_length);
}
   
/**
 * \brief Destroys attribute object.
 *
 * Just to help garbage collector.
 */
Attribute::~Attribute()
{
  if (value != NULL) {
    free(value);
  }
}

/**
 * \brief Overloaded assignment operator.
 * \param rhs Right-hand-side operand.
 * \return Pointer to object.
 *
 * Assignment operator must be overloaded because of dynamically allocated resources.
 */
Attribute &Attribute::operator= (Attribute &rhs)
{
  if (this != &rhs) {
    if (this->value) {
      free(this->value);
    }
    this->type = rhs.type;
    this->length = rhs.length;
    uint16_t value_length = rhs.get_value_length();
    this->value = (uint8_t *) memalloc(NULL, value_length);
    if (this->value) {
      memcpy(this->value, rhs.value, value_length);
    } else {
      this->length = 0;
    }
  }
  return *this;
}

/**
 * \brief Gets the value of an attribute.
 * \param value Pointer to become the beginning of the value buffer.
 * \param length Size of value buffer will be copied here.
 *
 * Provides a pointer to the buffer that holds the value plus its size.
 */
void Attribute::get_value(uint8_t *&value, uint16_t &length) {
  value = this->value;
  length = get_value_length();
}

/**
 * \brief Sets the value of an attribute.
 * \param bytes Pointer to the buffers that holds the data to become the value of the
 * attribute.
 * \param value_length Size of value (in bytes).
 *
 * Copies the content of the buffer into the value field of the attribute object.
 */
void Attribute::set_value(uint8_t *bytes, uint16_t value_length)
{
  value = (uint8_t *) memalloc(value, value_length);
  if (value != NULL) {
    memcpy(value, bytes, value_length);
    length = sizeof(type) + sizeof(length) + value_length;
  }
}
  
/**
 * \brief Get the length of the value field of an attribute.
 * \return Length of value field in bytes.
 *
 * Returns the length of the value field of an attribute, i.e., total attribute
 * length minus the length of the type and length fields.
 */
uint16_t Attribute::get_value_length()
{
  return (length - sizeof(type) - sizeof(length));
}

/**
 * \brief Converts attribute data into a raw string that can be inserted in a raw packet.
 * \return Pointer to the beginning of the raw attribute.
 *
 * Concatenates type, length and value fields to form a raw attribute that can be copied into
 * a packet.
 */
uint8_t *Attribute::encode()
{
  uint16_t network_length = htons(length);
  
  if (value != NULL) {
    uint8_t *raw_attribute = (uint8_t *) memalloc(NULL, length * sizeof(uint8_t));
    if (raw_attribute != NULL) {
      memcpy(raw_attribute, &type, sizeof(type));
      memcpy(raw_attribute + sizeof(type), &network_length, sizeof(network_length));
      memcpy(raw_attribute + sizeof(type) + sizeof(length), value, get_value_length());
    }
    return raw_attribute;
  }
  return NULL;
}

/**
 * \brief Creates a new packet with given application and code fields.
 * \param application GTLV application field.
 * \param code GTLV code field.
 *
 * Creates a packet with no data content.
 */
Packet::Packet(uint16_t application, uint8_t code): application((uint16_t) application), code((uint8_t) code), length(0)
{
}

/**
 * \brief Creates a new packet from a raw string.
 * \param raw_packet Pointer to the beginning of the raw string that holds the encoded packet.
 * \param length Size of the raw packet.
 *
 * Creates a packet object by decoding a raw string that typically comes from the network.
 */
Packet::Packet(uint8_t *raw_packet, uint16_t length)
{
  decode(raw_packet, length);
}

/**
 * \brief Adds a new boolean attribute to the packet.
 * \param type Attribute type.
 * \param value Attribute value.
 *
 * Adds attribute to packet based on separate type and value arguments.
 */
void Packet::add_attribute(uint8_t type, uint8_t value)
{
  Attribute attribute(type, value);
  actual_attributes.append(attribute);
}

/**
 * \brief Adds a new integer/timestamp attribute to the packet.
 * \param type Attribute type.
 * \param value Attribute value.
 *
 * Adds attribute to packet based on separate type and value arguments.
 */
void Packet::add_attribute(uint8_t type, uint32_t value)
{
  Attribute attribute(type, value);
  actual_attributes.append(attribute);
}

/**
 * \brief Adds a new octet/string attribute to the packet.
 * \param type Attribute type.
 * \param value Attribute value.
 * \param value_length Size of attribute value in bytes.
 *
 * Adds attribute to packet based on separate type, value and value size arguments.
 */
void Packet::add_attribute(uint8_t type, uint8_t *value, uint16_t value_length)
{
  Attribute attribute(type, value, value_length);
  actual_attributes.append(attribute);
}

/**
 * \brief Gets attribute list for a packet.
 * \return Attribute list.
 *
 * Gets a list of the attributes that are currently in the packet that is being constructed.
 */
List <Attribute> *Packet::get_attributes()
{
  return &actual_attributes;
}

/**
 * \brief Converts packet data into a raw string that can be sent over the network.
 * \param raw_packet Pointer that will point to the (re)allocated buffer that will hold the raw packet.
 * \return Size of the buffer.
 *
 * Concatenates packet regular fields plus attributes to form a raw packet that can be sent
 * over the network.
 */
uint16_t Packet::encode(uint8_t *&raw_packet)
{
  Attribute *current;
  uint8_t *next_fill;
  uint16_t total_length = 0;
  
  actual_attributes.rewind();
  while ((current = actual_attributes.next())) {
    total_length += current->length;
  }

  total_length += (sizeof(application) + sizeof(code) + sizeof(total_length));

  raw_packet = (uint8_t *) memalloc(raw_packet, total_length * sizeof(uint8_t));
  if (raw_packet) {
    next_fill = raw_packet;

    uint16_t network_application = htons(application);
    memcpy(next_fill, &network_application, sizeof(network_application));
    next_fill += sizeof(application);

    memcpy(next_fill, &code, sizeof(code));
    next_fill += sizeof(code);

    uint16_t network_total_length = htons(total_length);
    memcpy(next_fill, &network_total_length, sizeof(network_total_length));
    next_fill += sizeof(total_length);

    actual_attributes.rewind();
    while ((current = actual_attributes.next())) {
      uint8_t *raw_attribute = current->encode();
      memcpy(next_fill, raw_attribute, current->length);
      next_fill += current->length;
      free(raw_attribute);
    }

    return total_length;
  } else {
    return 0;
  }
}

/**
 * \brief Identifies packet data fields, including attributes, in a raw string.
 * \param raw_packet Pointer to the beginning of a buffer that holds the raw packet.
 * \param bytes_to_read Size of the buffer.
 *
 * This method extracts application, code and packet length fields plus all the individual
 * attributes that can be found in a raw_string that typically comes directly from the network.
 */
void Packet::decode(uint8_t *raw_packet, size_t bytes_to_read)
{
  Packet *packet_decode_helper = (Packet *) raw_packet;
  Attribute *attribute_decode_helper;
  application = ntohs(packet_decode_helper->application);

  code = packet_decode_helper->code;

  length = ntohs(packet_decode_helper->length);

  raw_packet += (sizeof(application) + sizeof(code) + sizeof(length));
  bytes_to_read = length - (sizeof(application) + sizeof(code) + sizeof(length));
  while (bytes_to_read > 0) {
    attribute_decode_helper = (Attribute *) raw_packet;
    size_t attribute_length = ntohs(attribute_decode_helper->length);
    size_t value_length = attribute_length - (sizeof(attribute_decode_helper->type) + sizeof(attribute_decode_helper->length));
    if (attribute_length <= bytes_to_read) {
      Attribute current(attribute_decode_helper->type, (uint8_t *) attribute_decode_helper + sizeof(attribute_decode_helper->type) + sizeof(attribute_decode_helper->length), value_length);
      actual_attributes.append(current);
      raw_packet += current.length;
      bytes_to_read -= current.length;
    } else {
      bytes_to_read = 0;
    }
  }
}
