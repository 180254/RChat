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

    private Text text(String value, String cssClass) {
        Text text = new Text(value);
        text.getStyleClass().add("c-ct-message");

        if (cssClass != null) {
            text.getStyleClass().add(cssClass);
        }

        return text;
    }

    public List<Text> toNodes() {
        List<Text> ret = new ArrayList<>();

        ret.add(text(dtf.format(time), null));
        ret.add(text(" <", null));
        ret.add(text(user, "c-ct-message-user"));
        ret.add(text("> ", null));
        ret.add(text(message, null));
        ret.add(text("\n", null));

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
