package org.diameter4j.base;

import org.apache.commons.net.ntp.TimeStamp;
import org.diameter4j.AVP;
import org.diameter4j.DataFormat;
import org.diameter4j.Type;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.diameter4j.Factory.*;

public abstract class Common {

    public static final int IETF_VENDOR_ID = 0;

    public static DataFormat<List<AVP<?>>> grouped = new DataFormat<List<AVP<?>>>("Grouped") {

        public List<AVP<?>> decode(ByteBuffer buffer) throws IOException {
           List<AVP<?>> avps = new ArrayList<>();
           while (buffer.hasRemaining()) {
              // TODO
           }
           return avps;
        }

        public ByteBuffer encode(ByteBuffer buffer, List<AVP<?>> value) throws IOException {
            return buffer;
        }
    };

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

    public static final DataFormat<Long> unsigned32 = new DataFormat<Long>("unsigned32") {

        public Long decode(ByteBuffer buffer) throws IOException {
            return buffer.getInt() & 0xFFFFFFFFl;
        }

        public ByteBuffer encode(ByteBuffer buffer, Long value) throws IOException {
            return ensureSpace(buffer, 4).putInt(value.intValue());
        }
    };

    public static class EnumDataFormat<E extends Enum<E>> extends DataFormat<E> {

        private E[] enums;
        private int offset;

        public EnumDataFormat(Class<E> clazz) {
            super("Enumerated: " + clazz.getSimpleName());
            EnumSet<E> values = EnumSet.allOf(clazz);
            enums = (E[]) Array.newInstance(clazz, values.size());
            values.toArray(enums);
        }

        public EnumDataFormat(Class<E> clazz, int offset) {
            this(clazz);
            this.offset = offset;
        }

        public E decode(ByteBuffer buffer) throws IOException {
            int index = unsigned32.decode(buffer).intValue();
            return enums[index-offset];
        }

        public ByteBuffer encode(ByteBuffer buffer, E value) throws IOException {
            return unsigned32.encode(buffer, (long) (value.ordinal() + offset));
        }
    }

    public static Type<Long> newUnsigned32Type(String name, int code) {
        return newType(name, code, unsigned32);
    }

    public static <T extends Enum<T>> Type<T> newEnumType(String name, int code, Class<T> clazz) {
        return newEnumType(name, IETF_VENDOR_ID, code, clazz);
    }

    public static <T extends Enum<T>> Type<T> newEnumType(String name, int vendorId, int code, Class<T> clazz) {
        return newType(name, vendorId, code, new EnumDataFormat<T>(clazz));
    }

    // ======================== AVP Types ========================

    public static final int
            USER_NAME_ORDINAL = 1,
            AUTH_APPLICATION_ID_ORDINAL = 258,
            AUTH_SESSION_STATE_ORDINAL = 277,
            ACCT_APPLICATION_ID_ORDINAL = 259,
            SESSION_ID_ORDINAL = 263,
            DESTINATION_HOST_ORDINAL = 293,
            DESTINATION_REALM_ORDINAL = 283,
            DISCONNECT_CAUSE_ORDINAL = 273,
            ORIGIN_HOST_ORDINAL = 264,
            ORIGIN_REALM_ORDINAL = 296,
            ORIGIN_STATE_ID_ORDINAL= 278,
            FIRMWARE_REVISION_ORDINAL = 267,
            HOST_IP_ADDRESS_ORDINAL = 257,
            RESULT_CODE_ORDINAL = 268,
            SUPPORTED_VENDOR_ID_ORDINAL = 265,
            VENDOR_ID_ORDINAL = 266,
            VENDOR_SPECIFIC_APPLICATION_ID_ORDINAL = 260,
            PRODUCT_NAME_ORDINAL = 269,
            FAILED_AVP_ORDINAL = 279,
            ERROR_MESSAGE_ORDINAL = 281,
            ROUTE_RECORD_ORDINAL = 282,
            REDIRECT_HOST_ORDINAL = 292,
            ERROR_REPORTING_HOST = 294,
            EXPERIMENTAL_RESULT_ORDINAL = 297,
            EXPERIMENTAL_RESULT_CODE_ORDINAL = 298;

    public static enum AuthSessionState
    {
        /**
         * STATE_MAINTAINED 0.
         * <p> This value is used to specify that session state is
         * being maintained, and the access device MUST issue a session termination
         * message when service to the user is terminated. This is the default
         * value.
         */
        STATE_MAINTAINED,

        /**
         * NO_STATE_MAINTAINED 1
         * <p>This value is used to specify that no session
         * termination messages will be sent by the access device upon expiration of
         * the Authorization-Lifetime.
         */
        NO_STATE_MAINTAINED
    }

    /**
     * The Auth-Session-State AVP (AVP Code 277) is of type Enumerated and
     * specifies whether state is maintained for a particular session. The
     * client MAY include this AVP in requests as a hint to the server, but the
     * value in the server's answer message is binding.
     *
     * @see AuthSessionState
     */
    public static final Type<AuthSessionState> AUTH_SESSION_STATE = newEnumType(
            "Auth-Session-State", AUTH_SESSION_STATE_ORDINAL, AuthSessionState.class);


    /**
     * The Auth-Application-Id AVP (AVP Code 258) is of type Unsigned32 and
     * is used in order to advertise support of the Authentication and
     * Authorization portion of an application (see Section 2.4).  The
     * Auth-Application-Id MUST also be present in all Authentication and/or
     * Authorization messages that are defined in a separate Diameter
     * specification and have an Application ID assigned.
     */
    public static final Type<Long> AUTH_APPLICATION_ID = newUnsigned32Type(
            "Auth-Application-Id", AUTH_APPLICATION_ID_ORDINAL);


    /**
     * The Acct-Application-Id AVP (AVP Code 259) is of type Unsigned32 and
     * is used in order to advertise support of the Accounting portion of an
     * application (see Section 2.4).  The Acct-Application-Id MUST also be
     * present in all Accounting messages.  Exactly one of the Auth-
     * Application-Id and Acct-Application-Id AVPs MAY be present.
     */
    public static final Type<Long> ACCT_APPLICATION_ID = newUnsigned32Type(
            "Acct-Application-Id", ACCT_APPLICATION_ID_ORDINAL);

}
