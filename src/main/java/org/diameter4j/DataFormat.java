package org.diameter4j;

import org.diameter4j.io.AbstractCodec;

public abstract class DataFormat<T> extends AbstractCodec<T> {

    private final String name;

    public DataFormat(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
