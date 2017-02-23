package org.diameter4j;

import org.diameter4j.base.Common;

public class Type<T> {

    private int vendorId;
    private int code;
    private String name;
    private DataFormat<T> format;
    private boolean mandatory;

    public Type(int vendorId, int code, String name, DataFormat<T> format) {
        this.vendorId = vendorId;
        this.code = code;
        this.name = name;
        this.format = format;
        this.mandatory = true;
    }

    public DataFormat<T> getDataFormat() {
        return format;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public int getCode() {
        return code;
    }

    public boolean isVendorSpecific() {
        return vendorId != Common.IETF_VENDOR_ID;
    }

    public int getVendorId() {
        return vendorId;
    }

}
