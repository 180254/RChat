package pl.nn44.rchat.protocol;

public interface StatefulCmd {

    Response<?> accept(String session, String channel, String param, boolean state) throws ChatException;
}
