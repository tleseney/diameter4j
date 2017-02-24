package org.diameter4j;

import java.util.List;

public abstract class Message {

    protected Command command;

    protected int applicationId;
    protected int hopByHopId;
    protected int endToEndId;

    protected List<AVP<?>> avps;

    public abstract boolean isRequest();

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getHopByHopId() {
        return hopByHopId;
    }

    public void setHopByHopId(int hopByHopId) {
        this.hopByHopId = hopByHopId;
    }

    public int getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(int endToEndId) {
        this.endToEndId = endToEndId;
    }

    public List<AVP<?>> getAVPList() {
        return avps;
    }

    public void setAVPList(List<AVP<?>> avps) {
        this.avps = avps;
    }
}
