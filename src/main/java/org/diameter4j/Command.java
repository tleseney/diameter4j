package org.diameter4j;

/**
 * Definition of command code.
 * <br/>
 * Each command Request/Answer pair is assigned a command code, and the sub-type (i.e., request or
 * answer) is identified via the 'R' bit in the Command Flags field of the Diameter header.
 * <p>
 * Every Diameter message MUST contain a command code in its header's Command-Code field, which is
 * used to determine the action that is to be taken for a particular message.
 * <p>
 * IETF command codes are defined in {@link org.diameter4j.base.Common}.
 * <br/>
 * 3GPP command codes are defined in {@link org.cipango.diameter.ims.Cx},
 * {@link org.diameter4j.ims.Sh}, {@link org.diameter4j.ims.Zh}.
 */
public class Command {

    private boolean request;
    private int code;
    private String name;

    public Command(boolean request, int code, String name) {
        this.request = request;
        this.code = code;
        this.name = name;
    }

    public boolean isRequest() {
        return request;
    }

    public int getCode() {
        return code;
    }

    public String toString() {
        return name + "(" + code + ")";
    }
}
