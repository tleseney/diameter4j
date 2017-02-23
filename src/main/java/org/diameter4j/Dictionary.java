package org.diameter4j;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {

    private static final Dictionary instance = new Dictionary();

    public static Dictionary getInstance() {
        return instance;
    }

    private Map<Long, Type<?>> types = new HashMap<>();

    public Type<?> getType(int vendorId, int code) {
        return types.get(id(vendorId, code));
    }

    public long id(int vendorId, int code) {
        return (long) vendorId << 32 | code;
    }
}
