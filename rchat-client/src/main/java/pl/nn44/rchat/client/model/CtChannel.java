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
    private ObservableList<CtMessage> messages;
    private boolean join;
    private String currentMsg;

    public CtChannel(RcChannel channel) {
        this.name = channel.getName();
        this.users = FXCollections.observableArrayList();
        this.messages = FXCollections.observableArrayList();
        this.join = false;
        this.currentMsg = "";

        update(channel);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void update(RcChannel channel) {
        this.password = channel.isPassword();
        this.topic = channel.getTopic();

        this.users.clear();
        for (RcChUser user : channel.getRcChUsers()) {
            this.users.add(new CtUser(user));
        }
    }

    public void clear() {
        this.topic = "";
        this.users.clear();
        this.messages.clear();
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

    public ObservableList<CtMessage> getMessages() {
        return messages;
    }

    public void setMessages(ObservableList<CtMessage> messages) {
        this.messages = messages;
    }

    public boolean isJoin() {
        return join;
    }

    public void setJoin(boolean join) {
        this.join = join;
    }

    public String getCurrentMsg() {
        return currentMsg;
    }

    public void setCurrentMsg(String currentMsg) {
        this.currentMsg = currentMsg;
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
