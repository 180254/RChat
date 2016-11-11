package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.stream.Stream;

public class RcChannel implements Serializable {

    private static final long serialVersionUID = -868502509392109589L;

    private final String name;
    private final boolean password;
    private final String topic;
    private final RcChUser[] rcChUsers;

    public RcChannel(String name,
                     boolean password) {

        this.name = name;
        this.password = password;
        this.topic = null;
        this.rcChUsers = null;
    }

    public RcChannel(String name,
                     boolean password,
                     String topic,
                     RcChUser[] rcChUsers) {

        this.name = name;
        this.password = password;
        this.topic = topic;
        this.rcChUsers = rcChUsers.clone();
    }

    protected RcChannel() {
        this.name = null;
        this.password = false;
        this.topic = null;
        this.rcChUsers = null;
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

    public RcChUser[] getRcChUsers() {
        return rcChUsers.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RcChannel channel = (RcChannel) o;
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
                .add("rcChUsers", Stream.of(
                        rcChUsers != null ? rcChUsers : new RcChUser[0]
                ).map(RcChUser::getUsername))
                .toString();
    }
}
