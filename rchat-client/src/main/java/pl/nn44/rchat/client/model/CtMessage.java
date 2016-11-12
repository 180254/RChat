package pl.nn44.rchat.client.model;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CtMessage {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:MM:SS");

    private final String user;
    private final LocalDateTime time;
    private final String message;

    public CtMessage(String user, LocalDateTime time, String message) {
        this.user = user;
        this.time = time;
        this.message = message;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getUser() {
        return user;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return MessageFormat.format(
                "{0} {1}: {2}",
                dtf.format(time),
                user,
                message
        );
    }
}
