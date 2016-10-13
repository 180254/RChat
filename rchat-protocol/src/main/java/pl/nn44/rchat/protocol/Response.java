package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class Response<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -9109934797251792407L;

    private final Status status;
    private final T payload;

    public Response(Status status) {
        this.status = status;
        this.payload = null;
    }

    public Response(Status status, T payload) {
        this.status = status;
        this.payload = payload;
    }

    public Status getStatus() {
        return status;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response<?> response = (Response<?>) o;
        return status == response.status &&
                Objects.equal(payload, response.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(status, payload);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("payload", payload)
                .toString();
    }
}
