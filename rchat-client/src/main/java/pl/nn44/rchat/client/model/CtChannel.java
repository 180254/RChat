package pl.nn44.rchat.client.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pl.nn44.rchat.protocol.RcChannel;

import java.text.MessageFormat;

public class CtChannel {

    private RcChannel channel;
    private boolean join;

    private String topic;
    private final ObservableList<CtUser> oUsers = FXCollections.observableArrayList();

    public CtChannel(RcChannel channel) {
        this.channel = channel;
        this.join = false;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public RcChannel getChannel() {
        return channel;
    }

    public void setChannel(RcChannel channel) {
        this.channel = channel;
    }

    public boolean isJoin() {
        return join;
    }

    public void setJoin(boolean join) {
        this.join = join;
    }

    public ObservableList<CtUser> getoUsers() {
        return oUsers;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String join = this.join ? "[+]" : "[-]";

        String modes = "(";
        modes += channel.isPassword() ? "p" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                channel.getName(),
                modes
        ).trim();
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
