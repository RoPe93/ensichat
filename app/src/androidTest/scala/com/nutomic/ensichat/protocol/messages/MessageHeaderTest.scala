package com.nutomic.ensichat.protocol.messages

import android.test.AndroidTestCase
import com.nutomic.ensichat.protocol.messages.MessageHeaderTest._
import com.nutomic.ensichat.protocol.{Address, AddressTest}
import junit.framework.Assert._

object MessageHeaderTest {

  val h1 = new MessageHeader(Text.Type, MessageHeader.DefaultHopLimit, AddressTest.a1,
    AddressTest.a2, 5, 1234, 0)

  val h2 = new MessageHeader(Text.Type, 0, AddressTest.a1, AddressTest.a3, 30000, 8765, 234)

  val h3 = new MessageHeader(Text.Type, 0xff, AddressTest.a4, AddressTest.a2, 250, 0, 56)

  val h4 = new MessageHeader(0xfff, 0, Address.Null, Address.Broadcast, MessageHeader.SeqNumRange.last, 0, 0xff)

  val h5 = new MessageHeader(ConnectionInfo.Type, 0xff, Address.Broadcast, Address.Null, 0, 0xffff, 0)

  val headers = Set(h1, h2, h3, h4, h5)

}

class MessageHeaderTest extends AndroidTestCase {

  def testSerialize(): Unit = {
    headers.foreach{h =>
      val bytes = h.write(0)
      val header = MessageHeader.read(bytes)
      assertEquals(h, header)
      assertEquals(bytes.length, header.length)
    }
  }

}
