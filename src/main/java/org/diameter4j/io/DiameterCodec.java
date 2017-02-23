package org.diameter4j.io;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface DiameterCodec<T> {

    T decode(ByteBuffer buffer) throws IOException;
    ByteBuffer encode(ByteBuffer buffer, T value) throws IOException;
}
