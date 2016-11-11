package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.stream.Stream;

public class RChannel implements Serializable {

    private static final long serialVersionUID = -868502509392109589L;

    private final String name;
    private final boolean password;
    private final String topic;
    private final RChUser[] rChUsers;

    public RChannel(String name, boolean password) {
        this.name = name;
        this.password = password;
        this.topic = null;
        this.rChUsers = null;
    }

    public RChannel(String name, boolean password,
                    String topic, RChUser[] rChUsers) {
        this.name = name;
        this.password = password;
        this.topic = topic;
        this.rChUsers = rChUsers;
    }

    protected RChannel() {
        this.name = null;
        this.password = false;
        this.topic = null;
        this.rChUsers = null;
    }

    public String getName() {
        return name;
    }

    public boolean isPassword() {
        return password;
    }

    public String getTopic() {
        return topic;
    }

    public RChUser[] getRChUsers() {
        return rChUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RChannel channel = (RChannel) o;
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
                .add("rChUsers", Stream.of(
                        rChUsers != null ? rChUsers : new RChUser[0]
                ).map(RChUser::getUsername))
                .toString();
    }
}
