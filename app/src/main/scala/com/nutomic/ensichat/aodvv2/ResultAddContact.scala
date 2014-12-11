package com.nutomic.ensichat.aodvv2

import java.nio.ByteBuffer

import com.nutomic.ensichat.util.BufferUtils

object ResultAddContact {

  val Type = 5

  /**
   * Constructs [[ResultAddContact]] instance from byte array.
   */
  def read(array: Array[Byte]): ResultAddContact = {
    val b = ByteBuffer.wrap(array)
    val first = BufferUtils.getUnsignedByte(b)
    val accepted = (first & 0x80) != 0
    new ResultAddContact(accepted)
  }

}

/**
 * Contains the result of a [[RequestAddContact]] message.
 */
class ResultAddContact(val Accepted: Boolean) extends MessageBody {

  override def Type = ResultAddContact.Type

  override def write: Array[Byte] = {
    val b = ByteBuffer.allocate(length)
    BufferUtils.putUnsignedByte(b, if (Accepted) 0x80 else 0)
    (0 to 1).foreach(_ => BufferUtils.putUnsignedByte(b, 0))
    b.array()
  }

  override def toString = "ResultAddContact(Accepted=" + Accepted + ")"

  override def length = 4

}
