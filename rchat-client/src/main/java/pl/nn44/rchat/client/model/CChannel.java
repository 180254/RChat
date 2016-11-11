package pl.nn44.rchat.client.model;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import pl.nn44.rchat.protocol.RChannel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class CChannel implements Observable {

    private List<InvalidationListener> aa = new ArrayList<>();
    private RChannel rChannel;
    private final SimpleBooleanProperty join;

    public CChannel(RChannel rChannel) {
        this.rChannel = rChannel;
        this.join = new SimpleBooleanProperty();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public RChannel getRChannel() {
        return rChannel;
    }

    public void setRChannel(RChannel rChannel) {
        this.rChannel = rChannel;
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
        modes += rChannel.isPassword() ? "p" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                rChannel.getName(),
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
