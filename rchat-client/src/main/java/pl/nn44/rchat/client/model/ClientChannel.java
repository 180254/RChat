package pl.nn44.rchat.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import pl.nn44.rchat.protocol.model.Channel;
import pl.nn44.rchat.protocol.model.User;

import java.text.MessageFormat;

public class ClientChannel {

    private final String name;
    private final boolean password;

    private final SimpleStringProperty topic;
    private final ObservableList<ClientUser> users;
    private final ObservableList<Text> messages;

    private boolean join;
    private String sendCache;
    private boolean unread;

    // ---------------------------------------------------------------------------------------------------------------

    public ClientChannel(Channel channel) {
        this.name = channel.getName();
        this.password = channel.isPassword();

        this.topic = new SimpleStringProperty();
        this.users = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        this.messages = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

        this.join = false;
        this.sendCache = "";
        this.unread = false;

        update(channel);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void update(Channel channel) {
        topic.setValue(channel.getTopic());

        users.clear();
        for (User user : channel.getUsers()) {
            users.add(new ClientUser(user));
        }
    }

    public void clear() {
        topic.setValue("");
        users.clear();
        messages.clear();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public boolean isPassword() {
        return password;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public SimpleStringProperty getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic.setValue(topic);
    }

    public ObservableList<ClientUser> getUsers() {
        return users;
    }

    public ObservableList<Text> getMessages() {
        return messages;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public boolean isJoin() {
        return join;
    }

    public void setJoin(boolean join) {
        this.join = join;
    }

    public String getSendCache() {
        return sendCache;
    }

    public void setSendCache(String sendCache) {
        this.sendCache = sendCache;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String join = this.join ? "[+]" : "[-]";

        String modes = "";
        modes += password ? "p" : "";

        if (modes.length() > 0) {
            modes = "(" + modes + ")";
        }

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                name,
                modes
        ).trim();
    }
}
