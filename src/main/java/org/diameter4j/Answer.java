package org.diameter4j;

public class Answer extends Message {

    public final boolean isRequest() {
        return false;
    }
}
