package org.diameter4j.io;


import org.diameter4j.base.Common;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;

public class DataFormatTest {

    private ByteBuffer buffer;

    @Before
    public void setUp() {
        buffer = ByteBuffer.allocate(1024);
    }

    @Test
    public void testOctetString() throws Exception {

        String s = "diameter";

        buffer = Common.octetString.encode(buffer, s.getBytes());
        buffer.flip();

        assertEquals(s, new String(Common.octetString.decode(buffer)));
    }

    @Test
    public void testUtf8String() throws Exception {
        String s = "Û�fjRPsl0ˆ¤";
        buffer = Common.utf8String.encode(buffer, s);
        buffer.flip();

        assertEquals(s, Common.utf8String.decode(buffer));
    }

    @Test
    public void testAddress() throws Exception {
        InetAddress addressIPv4 = InetAddress.getByName("127.0.0.1");
        buffer = Common.address.encode(buffer, addressIPv4);
        InetAddress addressIPv6 = InetAddress.getByName("[::1]");
        buffer = Common.address.encode(buffer, addressIPv6);

        buffer.flip();

        assertEquals(addressIPv4, Common.address.decode(buffer));
        assertEquals(addressIPv6, Common.address.decode(buffer));
    }

    @Test
    public void testUnsigned32() throws Exception {
        long l = 0xffffffffl;
        buffer = Common.unsigned32.encode(buffer, l);
        buffer.flip();

        assertEquals(l, Common.unsigned32.decode(buffer).longValue());

        ByteBuffer b = ByteBuffer.allocate(10);
        System.out.println(b);

        b.putInt(1);
        System.out.println(b);
    }



    @Test
    public void testTime() throws Exception {
        Date date = new Date();
        buffer = Common.time.encode(buffer, date);
        buffer.flip();

        assertEquals(date.getTime() / 1000, Common.time.decode(buffer).getTime() / 1000);
    }
}
