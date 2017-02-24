package org.diameter4j.io;

import org.diameter4j.AVP;
import org.diameter4j.Type;
import org.diameter4j.base.Common;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import static org.junit.Assert.*;

public class AVPCodecTest {

    private ByteBuffer buffer;
    private DiameterCodec<AVP<?>> codec;

    @Before
    public void setUp() {
        buffer = ByteBuffer.allocate(64);
        codec = new AVPCodec();
    }

    @Test
    public void testAVPCodec() throws Exception {
        AVP<String> avp = new AVP(Common.PRODUCT_NAME, "diameter4j");

        buffer = codec.encode(buffer, avp);
        buffer.flip();

        AVP<?> decoded = codec.decode(buffer);
        assertEquals(avp.getType(), decoded.getType());
        assertEquals(avp.getValue(), decoded.getValue());
    }

    @Test
    public void testPadding() throws Exception {

        Type<byte[]> type = new Type<byte[]>(123, 456, "test", Common.octetString);
        AVP<byte[]> avp = new AVP<byte[]>(type, new byte[] { 13 });

        for (int i = 0; i < 64; i++)
            buffer.put((byte) 44);
        buffer.position(0);

        buffer = codec.encode(buffer, avp);

        ByteBuffer view = buffer.duplicate();
        view.position(view.position() - 3);
        for (int i = 0; i < 3; i++)
            assertEquals(0, view.get());

        buffer.flip();
        AVP<byte[]> decoded = (AVP<byte[]>) codec.decode(buffer);
        assertEquals(avp.getType().getVendorId(), decoded.getType().getVendorId());
        assertEquals(avp.getType().getCode(), decoded.getType().getCode());
        assertArrayEquals(avp.getValue(), decoded.getValue());
    }
}
