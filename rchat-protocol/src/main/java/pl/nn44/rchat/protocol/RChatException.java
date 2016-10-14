package pl.nn44.rchat.protocol;

public class RChatException extends Exception {

    private static final long serialVersionUID = -781231732637496618L;

    private Reason reason;

    public RChatException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        NOT_LOGGED_IN,
        BAD_PASSWORD,
        NOT_INSIDE,
        NO_PERMISSION,
        NO_SUCH_USER,
        ERROR_BANNED,
    }
}
