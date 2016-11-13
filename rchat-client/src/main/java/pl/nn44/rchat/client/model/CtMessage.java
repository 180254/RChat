package pl.nn44.rchat.client.model;

import javafx.scene.text.Text;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CtMessage extends Text {

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

    public List<Text> toNodes() {
        List<Text> ret = new ArrayList<>();

        Text text = new Text(dtf.format(time));
        text.getStyleClass().add("c-ct-message");
        ret.add(text);

        text = new Text(" <");
        text.getStyleClass().add("c-ct-message");
        ret.add(text);

        text = new Text(user);
        text.getStyleClass().add("c-ct-message");
        text.getStyleClass().add("c-ct-message-user");
        ret.add(text);

        text = new Text("> ");
        text.getStyleClass().add("c-ct-message");
        ret.add(text);

        text = new Text(message);
        text.getStyleClass().add("c-ct-message");
        ret.add(text);

        text = new Text("\n");
        text.getStyleClass().add("c-ct-message");
        ret.add(text);

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
