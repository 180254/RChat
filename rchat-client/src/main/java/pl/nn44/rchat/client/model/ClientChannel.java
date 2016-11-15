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
    private boolean password;
    private SimpleStringProperty topic;
    private ObservableList<ClientUser> users;
    private ObservableList<Text> messages;
    private boolean join;
    private String currentMsg;

    public ClientChannel(Channel channel) {
        this.name = channel.getName();
        this.topic = new SimpleStringProperty();
        this.users = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        this.messages = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        this.join = false;
        this.currentMsg = "";

        update(channel);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void update(Channel channel) {
        this.password = channel.isPassword();
        this.topic.setValue(channel.getTopic());

        this.users.clear();
        for (User user : channel.getUsers()) {
            this.users.add(new ClientUser(user));
        }
    }

    public void clear() {
        this.topic.setValue("");
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

    /*
    public void setPassword(boolean password) {
        this.password = password;
    }
    */

    public SimpleStringProperty getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic.setValue(topic);
    }

    public ObservableList<ClientUser> getUsers() {
        return users;
    }

    /*
    public void setUsers(ObservableList<ClientUser> users) {
        this.users = users;
    }
    */

    public ObservableList<Text> getMessages() {
        return messages;
    }

    /*
    public void setMessages(ObservableList<Text> messages) {
        this.messages = messages;
    }
    */

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
