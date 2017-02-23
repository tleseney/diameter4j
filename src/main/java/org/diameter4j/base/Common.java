package org.diameter4j.base;

import org.apache.commons.net.ntp.TimeStamp;
import org.diameter4j.DataFormat;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public abstract class Common {

    public static final int IETF_VENDOR_ID = 0;

    /**
     * The data contains arbitrary data of variable length.
     */
    public static final DataFormat<byte[]> octetString = new DataFormat<byte[]>("OctetString") {

        public byte[] decode(ByteBuffer buffer) throws IOException {
            byte[] bytes = new byte[buffer.limit() - buffer.position()];
            buffer.get(bytes);
            return bytes;
        }

        public ByteBuffer encode(ByteBuffer buffer, byte[] value) throws IOException {
            return ensureSpace(buffer, value.length).put(value);
        }
    };


    /**
     * The UTF8String format is derived from the OctetString AVP Base Format.
     * This is a human readable string represented using the
     * ISO/IEC IS 10646-1 character set, encoded as an OctetString using
     * the UTF-8 [UFT8] transformation format described in RFC 2279.
     */
    public static final DataFormat<String> utf8String = new DataFormat<String>("utf8String") {

        public String decode(ByteBuffer buffer) throws IOException {
            if (buffer.hasArray()) {
                return new String(buffer.array(), buffer.position(), buffer.limit() - buffer.position(), StandardCharsets.UTF_8);
            } else {
                return new String(octetString.decode(buffer), StandardCharsets.UTF_8);
            }
        }

        public ByteBuffer encode(ByteBuffer buffer, String value) throws IOException {
            return octetString.encode(buffer, value.getBytes(StandardCharsets.UTF_8));
        }
    };

    public static final DataFormat<InetAddress> address = new DataFormat<InetAddress>("address") {

        public static final int IPV4 = 1;
        public static final int IPV6 = 2;

        public InetAddress decode(ByteBuffer buffer) throws IOException {
            int type = (buffer.get() & 0xff) << 8 | (buffer.get() & 0xff);
            byte[] b;
            if (type == IPV4)
                b = new byte[4];
            else if (type == IPV6)
                b = new byte[16];
            else
                throw new IOException("Unknown address type: " + type);
            buffer.get(b, 0, b.length);

            return InetAddress.getByAddress(b);
        }

        public ByteBuffer encode(ByteBuffer buffer, InetAddress addr) throws IOException {
            int type = (addr instanceof Inet4Address) ? IPV4 : IPV6;
            buffer = ensureSpace(buffer, 2 + (type == IPV4 ? 4 : 16));
            buffer.put((byte) 0); buffer.put((byte) type);

            buffer.put(addr.getAddress());
            return buffer;
        }
    };

    public static final DataFormat<Date> time = new DataFormat<Date>("time") {

        public Date decode(ByteBuffer buffer) throws IOException {
            long seconds = 0;
            for (int i = 0; i < 4; i++){
                seconds |= (buffer.get() & 0xFF) << ((3-i) * 8);
            }
            return new Date(TimeStamp.getTime(seconds << 32));
        }

        public ByteBuffer encode(ByteBuffer buffer, Date date) throws IOException {
            buffer = ensureSpace(buffer, 4);
            byte[] data = new byte[4];
            long ntpDate = TimeStamp.getNtpTime(date.getTime()).ntpValue();

            for (int i = 0; i < 4; i++) {
                data[i] = (byte) (0xFF & (ntpDate >> (56 - 8*i)));
            }
            return buffer.put(data);
        }
    };
}
