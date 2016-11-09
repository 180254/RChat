package pl.nn44.rchat.server.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.concurrent.CopyOnWriteArrayList;

public class Channel {

    private final String name;
    private final String password;
    private String topic;

    private final CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<User> admins = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<User> banned = new CopyOnWriteArrayList<>();

    public Channel(String name, String password) {
        this.name = name;
        this.password = password;
        this.topic = "";
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getTopic() {
        return topic;
    }

    public CopyOnWriteArrayList<User> getUsers() {
        return users;
    }

    public CopyOnWriteArrayList<User> getAdmins() {
        return admins;
    }

    public CopyOnWriteArrayList<User> getBanned() {
        return banned;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equal(name, channel.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("password", password)
                .add("users", users.stream().map(User::getUsername))
                .add("admins", admins.stream().map(User::getUsername))
                .add("banned", banned.stream().map(User::getUsername))
                .toString();
    }
}
