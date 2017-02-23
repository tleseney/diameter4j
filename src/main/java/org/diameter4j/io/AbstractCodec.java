package org.diameter4j.io;

import java.nio.ByteBuffer;

public abstract class AbstractCodec<T> implements DiameterCodec<T> {

    public ByteBuffer ensureSpace(ByteBuffer buffer, int space) {
        if (buffer.remaining() < space) {
            while (space < (buffer.capacity() / 2) && space < 128) {
                space = space * 2;
            }

            ByteBuffer larger = ByteBuffer.allocate(buffer.capacity() + space);
            buffer.flip();
            larger.put(buffer);
            larger.position(buffer.position());
            return larger;
        }
        return buffer;
    }

}
