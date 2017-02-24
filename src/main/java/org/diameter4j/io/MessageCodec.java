package org.diameter4j.io;

import org.diameter4j.*;
import org.diameter4j.base.Common;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Version    |                 Message Length                |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | command flags |                  Command-Code                 |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Application-ID                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      Hop-by-Hop Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      End-to-End Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  AVPs ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-
 * </pre>
 */
public class MessageCodec extends AbstractCodec<Message> {

    public static final int DIAMETER_VERSION_1 = 1;
    public static final int REQUEST_FLAG = 0x80;
    public static final int PROXIABLE_FLAG = 0x40;

    public Message decode(ByteBuffer buffer) throws IOException {
        int i = buffer.getInt();

        int version = i >> 24 & 0xff;
        if (version != DIAMETER_VERSION_1)
            throw new IOException("Unsupported Diameter version: " + version);

        i = buffer.getInt();

        int flags = i >> 24 & 0xff;
        boolean isRequest = ((flags & REQUEST_FLAG) == REQUEST_FLAG);

        int code = i & 0xffffff;

        Dictionary dictionary = Dictionary.getInstance();

        Command command = isRequest ? dictionary.getRequest(code) : dictionary.getAnswer(code);
        if (command == null)
            command = isRequest ? Factory.newRequest(code, "Unknown") : Factory.newAnswer(code, "Unknown");

        Message message = isRequest ? new Request() : new Answer(); // TODO subclass ?

        message.setApplicationId(buffer.getInt());
        message.setHopByHopId(buffer.getInt());
        message.setEndToEndId(buffer.getInt());
        message.setCommand(command);

        message.setAVPList(Common.grouped.decode(buffer));

        return message;
    }

    public ByteBuffer encode(ByteBuffer buffer, Message value) throws IOException {
        return null;
    }
}
