package pl.nn44.rchat.client.impl;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import pl.nn44.rchat.protocol.RcChannel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class CtChannel implements Observable {

    private List<InvalidationListener> aa = new ArrayList<>();
    private RcChannel rcChannel;
    private final SimpleBooleanProperty join;

    public CtChannel(RcChannel rcChannel) {
        this.rcChannel = rcChannel;
        this.join = new SimpleBooleanProperty();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public RcChannel getRChannel() {
        return rcChannel;
    }

    public void setRChannel(RcChannel rcChannel) {
        this.rcChannel = rcChannel;
    }

    public boolean isJoin() {
        return join.get();
    }

    public void setJoin(boolean join) {
        aa.forEach(a -> a.invalidated(this));
        this.join.set(join);
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String join = this.join.get() ? "[+]" : "[-]";

        String modes = "(";
        modes += rcChannel.isPassword() ? "p" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                rcChannel.getName(),
                modes
        ).trim();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        aa.add(listener);

    }

    @Override
    public void removeListener(InvalidationListener listener) {
        aa.remove(listener);
    }
}
