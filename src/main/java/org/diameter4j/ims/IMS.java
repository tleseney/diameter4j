package org.diameter4j.ims;

import org.diameter4j.DataFormat;
import org.diameter4j.Factory;
import org.diameter4j.Type;

public abstract class IMS extends Factory {

    public static final int IMS_VENDOR_ID = 10415;

    protected static <T> Type<T> newIMSType(String name, int code, DataFormat<T> format) {
        return newType(name, IMS_VENDOR_ID, code, format);
    }

}
