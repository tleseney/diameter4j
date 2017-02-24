package org.diameter4j;

import org.diameter4j.base.Common;

import java.util.*;

/**
 * The Application Identifier is used to identify a specific Diameter Application. There are
 * standards-track application ids and vendor specific application ids.
 * <p>
 * IANA [IANA] has assigned the range 0x00000001 to 0x00ffffff for standards-track applications; and
 * 0x01000000 - 0xfffffffe for vendor specific applications, on a first-come, first-served basis.
 * The following values are allocated.
 * <ul>
 * <li>Diameter Common Messages 0
 * <li>NASREQ 1
 * <li>Mobile-IP 2
 * <li>Diameter Base Accounting 3
 * <li>Relay 0xffffffff
 * </ul>
 * <p>
 * Assignment of standards-track application IDs are by Designated Expert with Specification
 * Required [IANA].
 * <p>
 * Both Application-Id and Acct-Application-Id AVPs use the same Application Identifier space.
 * <p>
 * Vendor-Specific Application Identifiers, are for Private Use. Vendor-Specific Application
 * Identifiers are assigned on a First Come, First Served basis by IANA.
 * <p>
 * IETF applications ID: {@link org.diameter4j.base.Accounting#ACCOUNTING_ID Accounting}
 * <br/>
 * 3GPP applications ID: {@link org.diameter4j.ims.Sh#SH_APPLICATION_ID Sh},
 * {@link org.diameter4j.ims.Cx#CX_APPLICATION_ID Cx},
 * {@link org.diameter4j.ims.Zh#ZH_APPLICATION_ID Zh},
 * {@link org.diameter4j.ims.Zh#ZN_APPLICATION_ID Zn}
 */
public class ApplicationId {

    public enum Type {
        ACCT, AUTH;
    }

    private int id;
    private Type type;
    private Set<Integer> vendors;

    public ApplicationId(Type type, int id, Collection<Integer> vendors) {
        this.id = id;
        this.type = type;
        this.vendors = new HashSet<>(vendors);
    }

    public ApplicationId(Type type, int id, int vendor) {
        this(type, id, Collections.singletonList(vendor));
    }

    public ApplicationId(Type type, int id) {
        this(type, id, null);
    }

    public boolean isVendorSpecific() {
        return vendors != null  && !vendors.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o instanceof ApplicationId) {
            ApplicationId other = (ApplicationId) o;

            boolean equals = id == other.id && type == other.type && isVendorSpecific() == other.isVendorSpecific();
            if (equals && isVendorSpecific())
                equals = vendors.equals(other.vendors);

            return equals;
        }
        return false;
    }

    public static ApplicationId fromMessage(Message message) {
        Long appId = message.getValue(Common.ACCT_APPLICATION_ID);
        if (appId != null)
            return new ApplicationId(Type.ACCT, appId.intValue());

        appId = message.getValue(Common.AUTH_APPLICATION_ID);
        if (appId != null)
            return new ApplicationId(Type.AUTH, appId.intValue());

        AVPList vsai = message.getValue(Common.VENDOR_SPECIFIC_APPLICATION_ID);

        if (vsai == null)
            return null;

        List<Integer> vendors = new ArrayList<>();
        for (Long l : vsai.getValues(Common.VENDOR_ID))
            vendors.add(l.intValue());

        appId = vsai.getValue(Common.ACCT_APPLICATION_ID);
        if (appId != null)
            return new ApplicationId(Type.ACCT, appId.intValue(), vendors);

        appId = vsai.getValue(Common.AUTH_APPLICATION_ID);

        if (appId != null)
            return new ApplicationId(Type.AUTH, appId.intValue(), vendors);

        return null;
    }

    public String toString() {
        return id + (isVendorSpecific() ? vendors.toString() : "");
    }
}
