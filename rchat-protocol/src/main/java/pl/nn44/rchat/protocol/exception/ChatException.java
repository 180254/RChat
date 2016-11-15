package pl.nn44.rchat.protocol.exception;

public class ChatException extends Exception {

    private static final long serialVersionUID = -781231732637496618L;

    private final Reason reason;

    public ChatException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        ALREADY_LOGGED_IN,

        GIVEN_BAD_PASSWORD,
        GIVEN_BAD_SESSION,
        GIVEN_BAD_CHANNEL,
        GIVEN_BAD_USERNAME,

        NO_PERMISSION,
        UNWELCOME_BANNED
    }
}
