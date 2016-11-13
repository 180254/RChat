package pl.nn44.rchat.client.model;

import javafx.scene.text.Text;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CtMessage {

    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:MM:SS");

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

    public static Text text(String value, String... cssClasses) {
        Text text = new Text(value);
        text.getStyleClass().addAll(cssClasses);
        return text;
    }

    public List<Text> toNodes() {
        List<Text> ret = new ArrayList<>();

        ret.add(text(dtf.format(time), "c-ct-message"));
        ret.add(text(" <", "c-ct-message"));
        ret.add(text(user, "c-ct-message", "c-ct-message-user"));
        ret.add(text("> ", "c-ct-message"));
        ret.add(text(message, "c-ct-message"));
        ret.add(text("\n", "c-ct-message"));

        return ret;
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
