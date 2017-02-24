package org.diameter4j;

import org.diameter4j.base.Common;

import java.util.HashMap;
import java.util.Map;

public abstract class Factory {

    private Map<Long, Type<?>> types = new HashMap<>();

    public static <T> Type<T> newType(String name, int vendorId, int code, DataFormat<T> dataFormat)
    {
        return new Type<T>(vendorId, code, name, dataFormat);
    }

    public static <T> Type<T> newType(String name, int code, DataFormat<T> dataFormat)
    {
        return newType(name, Common.IETF_VENDOR_ID, code, dataFormat);
    }

    public static Command newCommand(boolean request, int code, String name, boolean proxiable)
    {
        return new Command(request, code, name);
    }

    public static Command newRequest(int code, String name)
    {
        return newCommand(true, code, name, true);
    }

    public static Command newAnswer(int code, String name)
    {
        return newCommand(false, code, name, true);
    }

}

