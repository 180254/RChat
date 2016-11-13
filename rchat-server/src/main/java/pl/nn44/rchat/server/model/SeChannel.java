package pl.nn44.rchat.server.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.concurrent.CopyOnWriteArrayList;

public class SeChannel {

    private final String name;
    private final String password;
    private String topic;

    private final CopyOnWriteArrayList<SeUser> users = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> admins = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> banned = new CopyOnWriteArrayList<>();

    public SeChannel(String name, String password) {
        this.name = name;
        this.password = password;
        this.topic = "";
    }

    public SeChannel(String name, String password, String topic) {
        this.name = name;
        this.password = password;
        this.topic = topic;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public CopyOnWriteArrayList<SeUser> getUsers() {
        return users;
    }

    public CopyOnWriteArrayList<String> getAdmins() {
        return admins;
    }

    public CopyOnWriteArrayList<String> getBanned() {
        return banned;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeChannel channel = (SeChannel) o;
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
                .add("users", users.stream().map(SeUser::getUsername).toArray())
                .add("admins", admins)
                .add("banned", banned)
                .toString();
    }
}
