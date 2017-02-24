package org.diameter4j;

import java.util.List;

public abstract class Message {

    protected Command command;

    protected int applicationId;
    protected int hopByHopId;
    protected int endToEndId;

    protected AVPList avps;

    public abstract boolean isRequest();

    public Command getCommand() {
        return command;
    }

    public <T> T getValue(Type<T> type) {
        return avps.getValue(type);
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

    public AVPList getAVPList() {
        return avps;
    }

    public void setAVPList(AVPList avps) {
        this.avps = avps;
    }
}
