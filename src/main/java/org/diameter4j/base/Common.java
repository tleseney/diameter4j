package org.diameter4j.base;

import org.apache.commons.net.ntp.TimeStamp;
import org.diameter4j.*;
import org.diameter4j.io.AVPCodec;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.diameter4j.Factory.*;

public abstract class Common {

    public static final int IETF_VENDOR_ID = 0;

    public static final DataFormat<AVPList> grouped = new DataFormat<AVPList>("grouped") {

        private AVPCodec avpCodec = new AVPCodec();

        public AVPList decode(ByteBuffer buffer) throws IOException {

            AVPList avps = new AVPList();
            while (buffer.hasRemaining()) {
              AVP<?> avp = avpCodec.decode(buffer);
              avps.add(avp);
            }
            return avps;
        }

        public ByteBuffer encode(ByteBuffer buffer, AVPList avps) throws IOException {
            for (AVP<?> avp : avps) {
                buffer = avpCodec.encode(buffer, avp);
            }
            return buffer;
        }
    };

    /**
     * The data contains arbitrary data of variable length.
     */
    public static final DataFormat<byte[]> octetString = new DataFormat<byte[]>("octetString") {

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

    public interface CustomEnumOrdinal {
        long getOrdinal();
    }

    public static class CustomEnumDataFormat<E extends Enum<E> & CustomEnumOrdinal> extends DataFormat<E> {

        private Map<Long, E> enums = new HashMap<>();

        public CustomEnumDataFormat(Class<E> clazz) {
            super("Enumerated: " + clazz.getSimpleName());
            EnumSet<E> values = EnumSet.allOf(clazz);

            for (E e : values) {
                enums.put(e.getOrdinal(), e);
            }
        }

        public E decode(ByteBuffer buffer) throws IOException {
            return enums.get(Common.unsigned32.decode(buffer));
        }

        public ByteBuffer encode(ByteBuffer buffer, E value) throws IOException {
            return Common.unsigned32.encode(buffer, value.getOrdinal());
        }
    }

    public static Type<AVPList> newGroupedType(String name, int code) {
        return newType(name, code, grouped);
    }

    public static Type<Long> newUnsigned32Type(String name, int code) {
        return newType(name, code, unsigned32);
    }

    public static Type<String> newUTF8StringType(String name, int code) {
        return newType(name, code, utf8String);
    }

    public static Type<InetAddress> newAddressType(String name, int code) {
        return newType(name, code, address);
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


    /**
     * The Origin-Host AVP (AVP Code 264) is of type DiameterIdentity, and MUST
     * be present in all Diameter messages. This AVP identifies the endpoint
     * that originated the Diameter message. Relay agents MUST NOT modify this
     * AVP. The value of the Origin-Host AVP is guaranteed to be unique within a
     * single host. Note that the Origin-Host AVP may resolve to more than one
     * address as the Diameter peer may support more than one address.
     */
    public static final Type<String> ORIGIN_HOST = newUTF8StringType("Origin-Host", ORIGIN_HOST_ORDINAL);

    /**
     * The Origin-Realm AVP (AVP Code 296) is of type DiameterIdentity. This AVP
     * contains the Realm of the originator of any Diameter message and MUST be
     * present in all messages.
     */
    public static final Type<String> ORIGIN_REALM = newUTF8StringType("Origin-Realm", ORIGIN_REALM_ORDINAL);

    /**
     * The Destination-Realm AVP (AVP Code 283) is of type DiameterIdentity, and
     * contains the realm the message is to be routed to. The Destination-Realm
     * AVP MUST NOT be present in Answer messages. Diameter Clients insert the
     * realm portion of the User-Name AVP. Diameter servers initiating a request
     * message use the value of the Origin-Realm AVP from a previous message
     * received from the intended target host (unless it is known a priori).
     * When present, the Destination-Realm AVP is used to perform message
     * routing decisions.
     * <p>
     * Request messages whose ABNF does not list the Destination-Realm AVP as a
     * mandatory AVP are inherently non-routable messages.
     * <p>
     * This AVP SHOULD be placed as close to the Diameter header as possible.
     */
    public static final Type<String> DESTINATION_REALM = newUTF8StringType(
            "Destination-Realm", DESTINATION_REALM_ORDINAL);

    /**
     * The Destination-Host AVP (AVP Code 293) is of type DiameterIdentity. This
     * AVP MUST be present in all unsolicited agent initiated messages, MAY be
     * present in request messages, and MUST NOT be present in Answer messages.
     * <p>
     * The absence of the Destination-Host AVP will cause a message to be sent
     * to any Diameter server supporting the application within the realm
     * specified in Destination-Realm AVP.
     * <p>
     * This AVP SHOULD be placed as close to the Diameter header as possible.
     */
    public static final Type<String> DESTINATION_HOST = newUTF8StringType(
            "Destination-Host", DESTINATION_HOST_ORDINAL);

    /**
     * The Session-Id AVP (AVP Code 263) is of type UTF8String and is used to
     * identify a specific session (see Section 8). All messages pertaining to a
     * specific session MUST include only one Session-Id AVP and the same value
     * MUST be used throughout the life of a session. When present, the
     * Session-Id SHOULD appear immediately following the Diameter Header (see
     * Section 3).
     * <p>
     * The Session-Id MUST be globally and eternally unique, as it is meant to
     * uniquely identify a user session without reference to any other
     * information, and may be needed to correlate historical authentication
     * information with accounting information. The Session-Id includes a
     * mandatory portion and an implementation-defined portion; a recommended
     * format for the implementation-defined portion is outlined below.
     * <p>
     * The Session-Id MUST begin with the sender's identity encoded in the
     * DiameterIdentity type (see Section 4.4). The remainder of the Session-Id
     * is delimited by a ";" character, and MAY be any sequence that the client
     * can guarantee to be eternally unique; however, the following format is
     * recommended, (square brackets [] indicate an optional element):
     * <p>
     *
     * <pre>
     * {@code <DiameterIdentity><high 32 bits>;<low 32 bits>[;<optional value>] }
     * </pre>
     * <p>
     * <high 32 bits> and <low 32 bits> are decimal representations of the high
     * and low 32 bits of a monotonically increasing 64-bit value. The 64-bit
     * value is rendered in two part to simplify formatting by 32-bit
     * processors. At startup, the high 32 bits of the 64-bit value MAY be
     * initialized to the time, and the low 32 bits MAY be initialized to zero.
     * This will for practical purposes eliminate the possibility of overlapping
     * Session-Ids after a reboot, assuming the reboot process takes longer than
     * a second. Alternatively, an implementation MAY keep track of the
     * increasing value in non-volatile memory.
     * <p>
     * <optional value> is implementation specific but may include a modem's
     * device Id, a layer 2 address, timestamp, etc.
     * <p>
     * Example, in which there is no optional value:
     * accesspoint7.acme.com;1876543210;523
     * <p>
     * Example, in which there is an optional value:
     * accesspoint7.acme.com;1876543210;523;mobile@200.1.1.88
     * <p>
     * The Session-Id is created by the Diameter application initiating the
     * session, which in most cases is done by the client. Note that a
     * Session-Id MAY be used for both the authorization and accounting commands
     * of a given application.
     */
    public static final Type<String> SESSION_ID = newUTF8StringType("Session-Id", SESSION_ID_ORDINAL);

    /**
     * The User-Name AVP (AVP Code 1) [RADIUS] is of type UTF8String, which
     * contains the User-Name, in a format consistent with the NAI specification
     * [NAI].
     */
    public static final Type<String> USER_NAME = newUTF8StringType("User-Name", USER_NAME_ORDINAL);

    /**
     * The Vendor-Specific-Application-Id AVP (AVP Code 260) is of type Grouped
     * and is used to advertise support of a vendor-specific Diameter
     * Application. Exactly one of the Auth-Application-Id and
     * Acct-Application-Id AVPs MAY be present.
     *
     * This AVP MUST also be present as the first AVP in all experimental
     * commands defined in the vendor-specific application.
     *
     * This AVP SHOULD be placed as close to the Diameter header as possible.
     *
     * <pre>
     * {@code
     * <Vendor-Specific-Application-Id> ::= < AVP Header: 260 >
     * 		1* [ Vendor-Id ]
     * 		0*1{ Auth-Application-Id }
     * 		0*1{ Acct-Application-Id }
     * }
     * </pre>
     */
    public static final Type<AVPList> VENDOR_SPECIFIC_APPLICATION_ID = newGroupedType(
            "Vendor-Specific-Application-Id", VENDOR_SPECIFIC_APPLICATION_ID_ORDINAL);

    /**
     * The Vendor-Id AVP (AVP Code 266) is of type Unsigned32 and contains the
     * IANA "SMI Network Management Private Enterprise Codes" [ASSIGNNO] value
     * assigned to the vendor of the Diameter application. In combination with
     * the Supported-Vendor-Id AVP (Section 5.3.6), this MAY be used in order to
     * know which vendor specific attributes may be sent to the peer. It is also
     * envisioned that the combination of the Vendor-Id, Product-Name (Section
     * 5.3.7) and the Firmware-Revision (Section 5.3.4) AVPs MAY provide very
     * useful debugging information.
     *
     * A Vendor-Id value of zero in the CER or CEA messages is reserved and
     * indicates that this field is ignored.
     */
    public static final Type<Long> VENDOR_ID = newUnsigned32Type("Vendor-Id", VENDOR_ID_ORDINAL);

    /**
     * The Product-Name AVP (AVP Code 269) is of type UTF8String, and contains
     * the vendor assigned name for the product. The Product-Name AVP SHOULD
     * remain constant across firmware revisions for the same product.
     */
    public static final Type<String> PRODUCT_NAME = newUTF8StringType("Product-Name", PRODUCT_NAME_ORDINAL).notMandatory();

    /**
     * The Host-IP-Address AVP (AVP Code 257) is of type Address and is used to
     * inform a Diameter peer of the sender's IP address. All source addresses
     * that a Diameter node expects to use with SCTP [SCTP] MUST be advertised
     * in the CER and CEA messages by including a Host-IP- Address AVP for each
     * address. This AVP MUST ONLY be used in the CER and CEA messages.
     */
    public static final Type<InetAddress> HOST_IP_ADDRESS = newAddressType("Host-IP-Address", HOST_IP_ADDRESS_ORDINAL);

    public static enum DisconnectCause
    {
        /** 0 A scheduled reboot is imminent. */
        REBOOTING,

        /**
         * 1 The peer's internal resources are constrained, and it has
         * determined that the transport connection needs to be closed.
         */
        BUSY,

        /**
         * 2 The peer has determined that it does not see a need for the
         * transport connection to exist, since it does not expect any
         * messages to be exchanged in the near future.
         */
        DO_NOT_WANT_TO_TALK_TO_YOU
    }

    /**
     * The Disconnect-Cause AVP (AVP Code 273) is of type Enumerated.  A
     * Diameter node MUST include this AVP in the Disconnect-Peer-Request
     * message to inform the peer of the reason for its intention to
     * shutdown the transport connection.  The following values are
     * supported:
     *
     * @see DisconnectCause
     */
    public static final Type<DisconnectCause> DISCONNECT_CAUSE = newEnumType("Disconnect-Clause",
            DISCONNECT_CAUSE_ORDINAL, DisconnectCause.class);

    /**
     * The Result-Code AVP (AVP Code 268) is of type Unsigned32 and
     * indicates whether a particular request was completed successfully or
     * whether an error occurred.  All Diameter answer messages defined in
     * IETF applications MUST include one Result-Code AVP.  A non-successful
     * Result-Code AVP (one containing a non 2xxx value other than
     * DIAMETER_REDIRECT_INDICATION) MUST include the Error-Reporting-Host
     * AVP if the host setting the Result-Code AVP is different from the
     * identity encoded in the Origin-Host AVP.
     */
    public static final Type<Long> RESULT_CODE = newUnsigned32Type("Result-Code", RESULT_CODE_ORDINAL);

    /**
     * The Experimental-Result AVP (AVP Code 297) is of type Grouped, and
     * indicates whether a particular vendor-specific request was completed
     * successfully or whether an error occurred.  Its Data field has the
     * following ABNF grammar:
     *
     * AVP Format
     * <pre>
     *    Experimental-Result ::= < AVP Header: 297 >
     *                               { Vendor-Id }
     *                               { Experimental-Result-Code }
     * </pre>
     *
     * The Vendor-Id AVP (see Section 5.3.3) in this grouped AVP identifies
     * the vendor responsible for the assignment of the result code which
     * follows.  All Diameter answer messages defined in vendor-specific
     * applications MUST include either one Result-Code AVP or one
     * Experimental-Result AVP.
     */
    public static final Type<AVPList> EXPERIMENTAL_RESULT = newGroupedType(
            "Experimental-Result", EXPERIMENTAL_RESULT_ORDINAL);

    /**
     * The Experimental-Result-Code AVP (AVP Code 298) is of type Unsigned32
     * and contains a vendor-assigned value representing the result of
     * processing the request.
     * <p>
     * It is recommended that vendor-specific result codes follow the same
     * conventions given for the Result-Code AVP regarding the different
     * types of result codes and the handling of errors (for non 2xxx
     * values).
     */
    public static final Type<Long> EXPERIMENTAL_RESULT_CODE = newUnsigned32Type(
            "Experimental-Result-Code", EXPERIMENTAL_RESULT_CODE_ORDINAL);


    /**
     * The Origin-State-Id AVP (AVP Code 278), of type Unsigned32, is a
     * monotonically increasing value that is advanced whenever a Diameter
     * entity restarts with loss of previous state, for example upon reboot.
     * Origin-State-Id MAY be included in any Diameter message, including
     * CER.
     *
     * A Diameter entity issuing this AVP MUST create a higher value for
     * this AVP each time its state is reset.  A Diameter entity MAY set
     * Origin-State-Id to the time of startup, or it MAY use an incrementing
     * counter retained in non-volatile memory across restarts.
     *
     * The Origin-State-Id, if present, MUST reflect the state of the entity
     * indicated by Origin-Host.  If a proxy modifies Origin-Host, it MUST
     * either remove Origin-State-Id or modify it appropriately as well.
     *
     * Typically, Origin-State-Id is used by an access device that always
     * starts up with no active sessions; that is, any session active prior
     * to restart will have been lost.  By including Origin-State-Id in a
     * message, it allows other Diameter entities to infer that sessions
     * associated with a lower Origin-State-Id are no longer active.  If an
     * access device does not intend for such inferences to be made, it MUST
     * either not include Origin-State-Id in any message, or set its value
     * to 0.
     */
    public static Type<Long> ORIGIN_STATE_ID = newUnsigned32Type("Origin-State-Id", ORIGIN_STATE_ID_ORDINAL);

    /**
     * The Firmware-Revision AVP (AVP Code 267) is of type Unsigned32 and is
     * used to inform a Diameter peer of the firmware revision of the
     * issuing device.
     * For devices that do not have a firmware revision (general purpose
     * computers running Diameter software modules, for instance), the
     * revision of the Diameter software module may be reported instead.
     */
    public static final Type<Long> FIRMWARE_REVISION =
            newUnsigned32Type("Firmware-Revision", FIRMWARE_REVISION_ORDINAL).notMandatory();

    /**
     * The Failed-AVP AVP (AVP Code 279) is of type Grouped and provides
     * debugging information in cases where a request is rejected or not
     * fully processed due to erroneous information in a specific AVP.  The
     * value of the Result-Code AVP will provide information on the reason
     * for the Failed-AVP AVP.
     *
     * The possible reasons for this AVP are the presence of an improperly
     * constructed AVP, an unsupported or unrecognized AVP, an invalid AVP
     * value, the omission of a required AVP, the presence of an explicitly
     * excluded AVP (see tables in Section 10), or the presence of two or
     * more occurrences of an AVP which is restricted to 0, 1, or 0-1
     * occurrences.
     *
     * A Diameter message MAY contain one Failed-AVP AVP, containing the
     * entire AVP that could not be processed successfully.  If the failure
     * reason is omission of a required AVP, an AVP with the missing AVP
     * code, the missing vendor id, and a zero filled payload of the minimum
     * required length for the omitted AVP will be added.
     *
     * <Failed-AVP> ::= < AVP Header: 279 >
     *               1* {AVP}
     */
    public static final Type<AVPList> FAILED_AVP = newGroupedType("Failed-AVP", FAILED_AVP_ORDINAL);

    /**
     * The Error-Message AVP (AVP Code 281) is of type UTF8String. It MAY accompany a Result-Code
     * AVP as a human readable error message. The Error-Message AVP is not intended to be useful in
     * real-time, and SHOULD NOT be expected to be parsed by network entities.
     */
    public static final Type<String> ERROR_MESSAGE_AVP = newUTF8StringType("Error-Message", ERROR_MESSAGE_ORDINAL).notMandatory();

    /**
     * The Route-Record AVP (AVP Code 282) is of type DiameterIdentity.  The
     * identity added in this AVP MUST be the same as the one received in
     * the Origin-Host of the Capabilities Exchange message.
     */
    public static final Type<String> ROUTE_RECORD_AVP = newUTF8StringType("Route-Record", ROUTE_RECORD_ORDINAL);

    /**
     * The Error-Reporting-Host AVP (AVP Code 294) is of type DiameterIdentity. This AVP contains
     * the identity of the Diameter host that sent the Result-Code AVP to a value other than 2001
     * (Success), only if the host setting the Result-Code is different from the one encoded in the
     * Origin-Host AVP. This AVP is intended to be used for troubleshooting purposes, and MUST be
     * set when the Result- Code AVP indicates a failure.
     */
    public static final Type<String> ERROR_REPORTING_HOST_AVP =
            newUTF8StringType("Error-Reporting-Host", ERROR_REPORTING_HOST).notMandatory();


    /**
     * The Supported-Vendor-Id AVP (AVP Code 265) is of type Unsigned32 and
     * contains the IANA "SMI Network Management Private Enterprise Codes"
     * [ASSIGNNO] value assigned to a vendor other than the device vendor.
     * This is used in the CER and CEA messages in order to inform the peer
     * that the sender supports (a subset of) the vendor-specific AVPs
     * defined by the vendor identified in this AVP.
     */
    public static final Type<Long> SUPPORTED_VENDOR_ID = newUnsigned32Type("Supported-Vendor-Id", SUPPORTED_VENDOR_ID_ORDINAL);

    /**
     * One or more of instances of this AVP MUST be present if the answer
     * message's 'E' bit is set and the Result-Code AVP is set to
     * DIAMETER_REDIRECT_INDICATION.
     *
     * Upon receiving the above, the receiving Diameter node SHOULD forward
     * the request directly to one of the hosts identified in these AVPs.
     * The server contained in the selected Redirect-Host AVP SHOULD be used
     * for all messages pertaining to this session.
     */
    public static final Type<String> REDIRECT_HOST = newUTF8StringType("Redirect-Host", REDIRECT_HOST_ORDINAL);


    // Radius for digest authentication (RFC 4590)
    public static final int
            DIGEST_REALM_ORDINAL = 104,
            DIGEST_QOP_ORDINAL = 110,
            DIGEST_ALGORITHM_ORDINAL = 111,
            DIGEST_HA1_ORDINAL = 121;

    /**
     * Description: This attribute describes a protection space component of the
     * RADIUS server. HTTP-style protocols differ in their definition of the
     * protection space. See [RFC2617], Section 1.2, for details. It MUST only
     * be used in Access-Request and Access-Challenge packets.
     *
     * Type: 104 for Digest-Realm
     *
     * Length: >=3
     *
     * Text: In Access-Requests, the RADIUS client takes the value of the realm
     * directive (realm-value according to [RFC2617]) without surrounding quotes
     * from the HTTP-style request it wants to authenticate. In Access-Challenge
     * packets, the RADIUS server puts the expected realm value into this
     * attribute.
     */
    public static final Type<String> DIGEST_REALM = newUTF8StringType("Digest-Realm", DIGEST_REALM_ORDINAL);

    /**
     * Description: This attribute holds the Quality of Protection parameter that
     * influences the HTTP Digest calculation. This attribute MUST only be used
     * in Access-Request and Access-Challenge packets. A RADIUS client SHOULD
     * insert one of the Digest-Qop attributes it has received in a previous
     * Access-Challenge packet. RADIUS servers SHOULD insert at least one
     * Digest-Qop attribute in an Access-Challenge packet. Digest-Qop is
     * optional in order to preserve backward compatibility with a minimal
     * implementation of [RFC2069].
     *
     * Text: In Access-Requests, the RADIUS client takes the value of the qop
     * directive (qop-value as described in [RFC2617]) from the HTTP-style
     * request it wants to authenticate. In Access-Challenge packets, the RADIUS
     * server puts a desired qop-value into this attribute. If the RADIUS server
     * supports more than one "quality of protection" value, it puts each
     * qop-value into a separate Digest-Qop attribute.
     */
    public static final Type<String> DIGEST_QOP = newUTF8StringType("Digest-Qop", DIGEST_QOP_ORDINAL);

    /**
     * Description This attribute holds the algorithm parameter that influences
     * the HTTP Digest calculation. It MUST only be used in Access-Request and
     * Access-Challenge packets. If this attribute is missing, MD5 is assumed.
     *
     * Text In Access-Requests, the RADIUS client takes the value of the
     * algorithm directive (as described in [RFC2617], section 3.2.1) from the
     * HTTP-style request it wants to authenticate. In Access-Challenge packets,
     * the RADIUS server SHOULD put the desired algorithm into this attribute.
     */
    public static final Type<String> DIGEST_ALGORITHM = newUTF8StringType("Digest-Algorithm", DIGEST_ALGORITHM_ORDINAL);

    /**
     * Description This attribute is used to allow the generation of an
     * Authentication-Info header, even if the HTTP-style response's body is
     * required for the calculation of the rspauth value. It SHOULD be used in
     * Access-Accept packets if the required quality of protection ('qop') is
     * 'auth-int'.
     *
     * This attribute MUST NOT be sent if the qop parameter was not specified or
     * has a value of 'auth' (in this case, use Digest-Response-Auth instead).
     *
     * The Digest-HA1 attribute MUST only be sent by the RADIUS server or
     * processed by the RADIUS client if at least one of the following
     * conditions is true:
     *
     * + The Digest-Algorithm attribute's value is 'MD5-sess' or
     * 'AKAv1-MD5-sess'.
     *
     * + IPsec is configured to protect traffic between RADIUS client and RADIUS
     * server with IPsec (see Section 8).
     *
     * This attribute MUST only be used in Access-Accept packets.
     *
     * Text This attribute contains the hexadecimal representation of H(A1) as
     * described in [RFC2617], sections 3.1.3, 3.2.1, and 3.2.2.2.
     */
    public static final Type<String> DIGEST_HA1 = newUTF8StringType("Digest-HA1", DIGEST_HA1_ORDINAL);



    // ======================== Commands ========================

    public static final int
            CER_ORDINAL = 257,
            CEA_ORDINAL = 257,
            DWR_ORDINAL = 280,
            DWA_ORDINAL = 280,
            DPR_ORDINAL = 282,
            DPA_ORDINAL = 282;

    /**
     * The Capabilities-Exchange-Request (CER), indicated by the Command-
     * Code set to 257 and the Command Flags' 'R' bit set, is sent to
     * exchange local capabilities.  Upon detection of a transport failure,
     * this message MUST NOT be sent to an alternate peer.
     *
     * When Diameter is run over SCTP [SCTP], which allows for connections
     * to span multiple interfaces and multiple IP addresses, the
     * Capabilities-Exchange-Request message MUST contain one Host-IP-
     * Address AVP for each potential IP address that MAY be locally used
     * when transmitting Diameter messages.
     * <pre>
     * {@code
     *  <CER> ::= < Diameter Header: 257, REQ >
     *           { Origin-Host }
     *           { Origin-Realm }
     *        1* { Host-IP-Address }
     *           { Vendor-Id }
     *           { Product-Name }
     *           [ Origin-State-Id ]
     *         * [ Supported-Vendor-Id ]
     *         * [ Auth-Application-Id ]
     *         * [ Inband-Security-Id ]
     *         * [ Acct-Application-Id ]
     *         * [ Vendor-Specific-Application-Id ]
     *           [ Firmware-Revision ]
     *         * [ AVP ]
     *  }
     *  </pre>
     */
    public static final Command CER = newCommand(true, CER_ORDINAL, "Capabilities-Exchange-Request", false);

    /**
     *  The Capabilities-Exchange-Answer (CEA), indicated by the Command-Code
     * set to 257 and the Command Flags' 'R' bit cleared, is sent in
     * response to a CER message.
     *
     * When Diameter is run over SCTP [SCTP], which allows connections to
     * span multiple interfaces, hence, multiple IP addresses, the
     * Capabilities-Exchange-Answer message MUST contain one Host-IP-Address
     * AVP for each potential IP address that MAY be locally used when
     * transmitting Diameter messages.
     *
     * Message Format
     * <pre>
     * {@code
     *  <CEA> ::= < Diameter Header: 257 >
     *          { Result-Code }
     *          { Origin-Host }
     *          { Origin-Realm }
     *       1* { Host-IP-Address }
     *          { Vendor-Id }
     *          { Product-Name }
     *          [ Origin-State-Id ]
     *          [ Error-Message ]
     *        * [ Failed-AVP ]
     *        * [ Supported-Vendor-Id ]
     *        * [ Auth-Application-Id ]
     *        * [ Inband-Security-Id ]
     *        * [ Acct-Application-Id ]
     *        * [ Vendor-Specific-Application-Id ]
     *          [ Firmware-Revision ]
     *        * [ AVP ]
     *  }
     *  </pre>
     */
    public static final Command CEA = newAnswer(CEA_ORDINAL, "Capabilities-Exchange-Answer");

    /**
     * <pre> {@code
     * 	<DWR>  ::= < Diameter Header: 280, REQ >
     * 		{ Origin-Host }
     * 		{ Origin-Realm }
     * 		[ Origin-State-Id ]
     * } </pre>
     *
     * @see Common#ORIGIN_HOST
     * @see Common#ORIGIN_REALM
     * @see Common#ORIGIN_STATE_ID
     */
    public static final Command DWR = newRequest(DWR_ORDINAL, "Device-Watchdog-Request");

    /**
     * <pre> {@code
     * <DWA>  ::= < Diameter Header: 280 >
     *           { Result-Code }
     *           { Origin-Host }
     *           { Origin-Realm }
     *           [ Error-Message ]
     *         * [ Failed-AVP ]
     *           [ Original-State-Id ]
     * } </pre>
     */
    public static final Command DWA = newAnswer(DWA_ORDINAL, "Device-Watchdog-Answer");

    /**
     * The Disconnect-Peer-Request (DPR), indicated by the Command-Code set
     * to 282 and the Command Flags' 'R' bit set, is sent to a peer to
     * inform its intentions to shutdown the transport connection.  Upon
     * detection of a transport failure, this message MUST NOT be sent to an
     * alternate peer.
     *
     * <pre> {@code
     * <DPR> ::= < Diameter Header: 282, REQ >
     *            { Origin-Host }
     *            { Origin-Realm }
     *            { Disconnect-Cause }
     * } </pre>
     */
    public static final Command DPR = newRequest(DPR_ORDINAL, "Disconnect-Peer-Request");

    /**
     * Upon receipt of this message, the transport connection is shutdown.
     *
     * <pre> {@code
     * <DPA>  ::= < Diameter Header: 282 >
     *            { Result-Code }
     *            { Origin-Host }
     *            { Origin-Realm }
     *            [ Error-Message ]
     *          * [ Failed-AVP ]
     * } </pre>
     */
    public static final Command DPA = newAnswer(DPA_ORDINAL, "Disconnect-Peer-Answer");
}
