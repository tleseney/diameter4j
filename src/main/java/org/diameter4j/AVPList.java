package org.diameter4j;

import java.util.AbstractList;
import java.util.ArrayList;

public class AVPList extends AbstractList<AVP<?>> {

    private ArrayList<AVP<?>> avps = new ArrayList<>();

    /**
     * Returns the first AVP of given type, null if not found
     */
    public <T> AVP<T> get(Type<T> type) {
        for (AVP<?> avp : avps) {
            if (avp.getType() == type)
                return (AVP<T>) avp;
        }
        return null;
    }

    /**
     * Returns the first AVP value of given type, null if not found
     */
    public <T> T getValue(Type<T> type) {
        AVP<T> avp = get(type);
        if (avp == null)
            return null;
        return avp.getValue();
    }

    @Override
    public void add(int index, AVP<?> avp) {
        avps.add(index, avp);
    }

    public AVP<?> get(int index) {
        return avps.get(index);
    }

    public int size() {
        return avps.size();
    }

}
