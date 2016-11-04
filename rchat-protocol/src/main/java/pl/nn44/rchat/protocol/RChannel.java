package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class RChannel implements Serializable {

    private static final long serialVersionUID = -868502509392109589L;

    private final String name;
    private final boolean password;

    public RChannel(String name, boolean password) {
        this.name = name;
        this.password = password;
    }

    protected RChannel() {
        this.name = null;
        this.password = false;
    }

    public String getName() {
        return name;
    }

    public boolean isPassword() {
        return password;
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
                .toString();
    }
}
