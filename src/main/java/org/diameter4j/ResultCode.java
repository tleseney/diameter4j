package org.diameter4j;

import org.diameter4j.base.Common;

public class ResultCode {

    private int vendorId;
    private int code;
    private String name;

    public ResultCode(int vendorId, int code, String name) {
        this.vendorId = vendorId;
        this.code = code;
        this.name = name;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * Return <code>true</code> if code is 1XXX.
     *
     * Errors that fall within this category are used to inform the requester that a request could
     * not be satisfied, and additional action is required on its part before access is granted.
     *
     * @return <code>true</code> if code is 1XXX.
     */
    public boolean isInformational() {
        return (code / 1000) == 1;
    }

    /**
     * Returns <code>true</code> if code is 2XXX.
     *
     * Errors that fall within the Success category are used to inform a peer that a request has
     * been successfully completed.
     *
     * @return <code>true</code> if code is 2XXX.
     */
    public boolean isSuccess() {
        return (code / 1000) == 2;
    }

    /**
     * Returns <code>true</code> if code is 3XXX.
     *
     * Errors that fall within the Protocol Error category SHOULD be treated on a per-hop basis, and
     * Diameter proxies MAY attempt to correct the error, if it is possible. Note that these and
     * only these errors MUST only be used in answer messages whose 'E' bit is set.
     *
     *
     * @return <code>true</code> if code is 3XXX.
     */
    public boolean isProtocolError() {
        return (code / 1000) == 3;
    }

    /**
     * Returns <code>true</code> if code is 4XXX.
     *
     * Errors that fall within the transient failures category are used to inform a peer that the
     * request could not be satisfied at the time it was received, but MAY be able to satisfy the
     * request in the future.
     *
     *
     * @return <code>true</code> if code is 4XXX.
     */
    public boolean isTransientFailure() {
        return (code / 1000) == 4;
    }

    /**
     * Returns <code>true</code> if code is 5XXX.
     *
     * Errors that fall within the permanent failures category are used to inform the peer that the
     * request failed, and should not be attempted again.
     *
     *
     * @return <code>true</code> if code is 5XXX.
     */
    public boolean isPermanentFailure() {
        return (code / 1000) == 5;
    }

    /**
     * Returns <code>true</code> if the AVP is an {@link org.diameter4j.base.Common#EXPERIMENTAL_RESULT_CODE}.
     */
    public boolean isExperimentalResultCode() {
        return vendorId != Common.IETF_VENDOR_ID;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (name != null)
            sb.append(name).append(' ');

        return sb.append("(").append(vendorId).append('/').append(code).append(")").toString();
    }
}
