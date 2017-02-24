package org.diameter4j.io;

import org.diameter4j.AVP;
import org.diameter4j.Dictionary;
import org.diameter4j.Factory;
import org.diameter4j.Type;
import org.diameter4j.base.Common;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <pre>
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           AVP Code                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V M P r r r r r|                  AVP Length                   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Vendor-ID (opt)                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Data ...
 * +-+-+-+-+-+-+-+-+
 * </pre>
 */
public class AVPCodec extends AbstractCodec<AVP<?>> {

    private static final int AVP_VENDOR_FLAG = 0x80;
    private static final int AVP_MANDATORY_FLAG = 0x40;

    public AVP<?> decode(ByteBuffer buffer) throws IOException {

        int code = buffer.getInt();
        int i = buffer.getInt();

        int flags = i >> 24 & 0xff;
        int length = i & 0xffffff;

        int dataLength = length - 8;
        int vendorId = 0;

        if ((flags & AVP_VENDOR_FLAG) == AVP_VENDOR_FLAG) {
            vendorId = buffer.getInt();
            dataLength -= 4;
        }

        ByteBuffer data = buffer.asReadOnlyBuffer();
        data.position(buffer.position());
        data.limit(data.position() + dataLength);

        buffer.position(buffer.position() + (dataLength + 3 & -4));

        Type<?> type = Dictionary.getInstance().getType(vendorId, code);

        if (type == null)
            type = Factory.newType("Unknown", vendorId, code, Common.octetString);

        AVP avp = new AVP(type);
        // TODO flags
        avp.setValue(type.getDataFormat().decode(data));

        return avp;
    }

    public ByteBuffer encode(ByteBuffer buffer, AVP avp) throws IOException {

        buffer = ensureSpace(buffer, 12);
        int flags = 0;

        if (avp.getType().isMandatory())
            flags |= AVP_MANDATORY_FLAG;

        int start = buffer.position();
        buffer.putInt(avp.getType().getCode());
        buffer.position(start + 8);

        if (avp.getType().isVendorSpecific()) {
            flags |= AVP_VENDOR_FLAG;
            buffer.putInt(avp.getType().getVendorId());
        }

        buffer = avp.getType().getDataFormat().encode(buffer, avp.getValue());
        buffer = ensureSpace(buffer, 8);

        buffer.putInt(start + 4, flags << 24 | (buffer.position() - start) & 0xffffff);
        while (buffer.position() % 4 != 0)
            buffer.put((byte) 0);

        return buffer;
    }
}
