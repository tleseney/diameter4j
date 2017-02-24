package org.diameter4j.io;

import org.junit.Test;

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

        ByteBuffer b = load("sar.dat");
    }
}
