package org.diameter4j;

public class Type<T> {

    private int vendorID;
    private int code;
    private String name;
    private DataFormat<T> format;
    private boolean mandatory;

}
