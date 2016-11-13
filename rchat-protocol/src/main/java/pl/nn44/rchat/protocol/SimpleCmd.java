package pl.nn44.rchat.protocol;

public interface SimpleCmd {

    Response<?> accept(String session, String channel, String param) throws ChatException;
}
