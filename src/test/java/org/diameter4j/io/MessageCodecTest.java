package org.diameter4j.io;

import org.diameter4j.Message;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.ByteBuffer;

public class MessageCodecTest {

    protected ByteBuffer load(String name) throws Exception {

        URL url = getClass().getClassLoader().getResource(name);
        File f = new File(url.getFile());
        FileInputStream fin = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        fin.read(b);
        fin.close();

        return ByteBuffer.wrap(b);
    }

    @Test
    public void testDecodeSAR() throws Exception {

        Message message = new MessageCodec().decode(load("sar.dat"));

        assertTrue(message.isRequest());
    }

    @Test
    public void testDecodeMAR() throws Exception {

        Message message = new MessageCodec().decode(load("mar.dat"));

        assertTrue(message.isRequest());
    }

    @Test
    public void testDecodeLIA() throws Exception {
        Message message = new MessageCodec().decode(load("lia.dat"));

        assertFalse(message.isRequest());

    }
}
