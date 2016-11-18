package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

public class Response<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -9109934797251792407L;

    private final T payload;

    // ---------------------------------------------------------------------------------------------------------------

    public Response() {
        this.payload = null;
    }

    public Response(T payload) {
        this.payload = payload;
    }

    public static Response<?> Ok() {
        return new Response();
    }

    public static <T extends Serializable> Response<T> Ok(T payload) {
        return new Response<>(payload);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public T getPayload() {
        return payload;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("payload", payload)
                .toString();
    }
}
