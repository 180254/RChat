package pl.nn44.rchat.client.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pl.nn44.rchat.protocol.RcChUser;
import pl.nn44.rchat.protocol.RcChannel;

import java.text.MessageFormat;

public class CtChannel {

    private final String name;
    private boolean password;
    private String topic;
    private ObservableList<CtUser> users;
    private boolean join;

    public CtChannel(RcChannel channel) {
        this.name = channel.getName();
        update(channel);
        this.join = false;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void update(RcChannel channel) {
        this.password = channel.isPassword();
        this.topic = channel.getTopic();

        users = FXCollections.observableArrayList();
        for (RcChUser user : channel.getRcChUsers()) {
            users.add(new CtUser(user));
        }
    }

    public void clear() {
        this.topic = "";
        this.users.clear();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public ObservableList<CtUser> getUsers() {
        return users;
    }

    public void setUsers(ObservableList<CtUser> users) {
        this.users = users;
    }

    public boolean isJoin() {
        return join;
    }

    public void setJoin(boolean join) {
        this.join = join;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String join = this.join ? "[+]" : "[-]";

        String modes = "(";
        modes += password ? "p" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                name,
                modes
        ).trim();
    }
}
