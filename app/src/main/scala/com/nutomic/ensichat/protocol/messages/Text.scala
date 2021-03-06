package com.nutomic.ensichat.protocol.messages

import java.nio.ByteBuffer
import java.util.Date

import com.nutomic.ensichat.protocol.BufferUtils

object Text {

  val Type = 6

  /**
   * Constructs [[Text]] instance from byte array.
   */
  def read(array: Array[Byte]): Text = {
    val b = ByteBuffer.wrap(array)
    val time = new Date(BufferUtils.getUnsignedInt(b) * 1000)
    val length = BufferUtils.getUnsignedInt(b).toInt
    val bytes = new Array[Byte](length)
    b.get(bytes, 0, length)
    new Text(new String(bytes, Message.Charset), time)
  }

}

/**
 * Holds a plain text message.
 */
case class Text(text: String, time: Date = new Date()) extends MessageBody {

  override def messageType = Text.Type

  override def write: Array[Byte] = {
    val b = ByteBuffer.allocate(length)
    BufferUtils.putUnsignedInt(b, time.getTime / 1000)
    val bytes = text.getBytes(Message.Charset)
    BufferUtils.putUnsignedInt(b, bytes.length)
    b.put(bytes)
    b.array()
  }

  override def length = 8 + text.getBytes(Message.Charset).length

  override def equals(a: Any): Boolean = a match {
    case o: Text => text == text && time.getTime / 1000 == o.time.getTime / 1000
    case _ => false
  }

}
