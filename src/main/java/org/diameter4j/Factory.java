package org.diameter4j;

import org.diameter4j.DataFormat;
import org.diameter4j.Type;
import org.diameter4j.base.Common;

public abstract class Factory {

    private Factory() {
    }

    public static <T> Type<T> newType(String name, int vendorId, int code, DataFormat<T> dataFormat)
    {
        return new Type<T>(vendorId, code, name, dataFormat);
    }

    public static <T> Type<T> newType(String name, int code, DataFormat<T> dataFormat)
    {
        return newType(name, Common.IETF_VENDOR_ID, code, dataFormat);
    }
}

