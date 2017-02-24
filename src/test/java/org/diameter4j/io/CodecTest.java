package org.diameter4j.io;

import org.diameter4j.base.Common;
import org.junit.Test;

import java.nio.ByteBuffer;
import static org.junit.Assert.*;

public class CodecTest {

    @Test
    public void testSize() throws Exception
    {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        for (int i = 0; i < 10000; i++) {
            buffer = Common.unsigned32.encode(buffer, (long) i);
        }
        buffer.flip();

        for (int i = 0; i < 10000; i++) {
            assertTrue(buffer.hasRemaining());
            assertEquals( i,  Common.unsigned32.decode(buffer).intValue());
        }
        assertFalse(buffer.hasRemaining());
    }
}
