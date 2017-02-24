package org.diameter4j.io;

import org.diameter4j.AVPList;
import org.diameter4j.Dictionary;
import org.diameter4j.Message;
import org.diameter4j.base.Common;
import org.diameter4j.ims.Cx;
import org.diameter4j.ims.IMS;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.ByteBuffer;

public class MessageCodecTest {

    @BeforeClass
    public static void setUpClass() {
        Dictionary.getInstance().load(Cx.class);
    }

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
        assertEquals(Cx.SAR, message.getCommand());
        assertEquals("scscf1.home1.net.home1.net;1140221717;10;my_session", message.getValue(Common.SESSION_ID));

        AVPList vsai = message.getValue(Common.VENDOR_SPECIFIC_APPLICATION_ID);
        assertEquals(IMS.IMS_VENDOR_ID, vsai.getValue(Common.VENDOR_ID).intValue());
        // TODO app id

        assertEquals(Common.AuthSessionState.NO_STATE_MAINTAINED, message.getValue(Common.AUTH_SESSION_STATE));
        assertEquals("scscf1.home1.net", message.getValue(Common.ORIGIN_HOST));
        assertEquals("home1.net", message.getValue(Common.ORIGIN_REALM));
        assertEquals("home1.net", message.getValue(Common.DESTINATION_REALM));
        assertEquals("sip:192.168.210.100:5090", message.getValue(Cx.SERVER_NAME));
        assertEquals(Cx.ServerAssignmentType.REGISTRATION, message.getValue(Cx.SERVER_ASSIGNMENT_TYPE));
        assertEquals(Cx.UserDataAlreadyAvailable.USER_DATA_NOT_AVAILABLE, message.getValue(Cx.USER_DATA_ALREADY_AVAILABLE));
        assertEquals("hss1.home1.net", message.getValue(Common.DESTINATION_HOST));
        assertEquals("user7@home1.net", message.getValue(Common.USER_NAME));
        assertEquals("sip:user7@home1.net", message.getValue(Cx.PUBLIC_IDENTITY));
    }

    @Test
    public void testDecodeMAR() throws Exception {

        Message message = new MessageCodec().decode(load("mar.dat"));

        assertTrue(message.isRequest());
        assertEquals(Cx.MAR, message.getCommand());
    }

    @Test
    public void testDecodeLIA() throws Exception {
        Message message = new MessageCodec().decode(load("lia.dat"));

        assertFalse(message.isRequest());
        assertEquals(Cx.LIA, message.getCommand());

    }
}
