package org.diameter4j;

public class Request extends Message {

    public final boolean isRequest() {
        return true;
    }
}
