package org.diameter4j;

public class AVP<T> {

    private Type<T> type;
    private T value;

    public AVP(Type<T> type) {
        this(type, null);
    }

    public AVP(Type<T> type, T value) {
        this.type = type;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public Type<T> getType() {
        return type;
    }

    public String toString() {
        return type + "=" + value;
    }
}
