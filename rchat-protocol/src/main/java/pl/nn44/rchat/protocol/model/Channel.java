package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.stream.Stream;

public class Channel implements Serializable {

    private static final long serialVersionUID = -868502509392109589L;

    private final String name;
    private final boolean password;
    private final String topic;
    private final User[] users;

    public Channel(String name,
                   boolean password,
                   String topic,
                   User[] users) {

        this.name = name;
        this.password = password;
        this.topic = topic;
        this.users = users.clone();
    }

    public Channel(String name,
                   boolean password) {

        this.name = name;
        this.password = password;
        this.topic = "";
        this.users = new User[0];
    }

    protected Channel() {
        this.name = null;
        this.password = false;
        this.topic = null;
        this.users = null;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public boolean isPassword() {
        return password;
    }

    public String getTopic() {
        return topic;
    }

    public User[] getUsers() {
        return users.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Channel channel = (Channel) obj;
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
                .add("topic", topic)
                .add("users", Stream.of(users).map(User::getUsername).toArray())
                .toString();
    }
}
