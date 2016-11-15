package pl.nn44.rchat.client.print;

import javafx.scene.Node;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrintMsg implements Printable {

    private final String user;
    private final LocalDateTime time;
    private final String message;

    public PrintMsg(String user, LocalDateTime time, String message) {
        this.user = user;
        this.time = time;
        this.message = message;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public List<Node> toNodes() {
        List<Node> ret = new ArrayList<>();
        ret.add(Print.txt(Print.time(time), "c-ct-message"));
        ret.add(Print.txt(" <", "c-ct-message"));
        ret.add(Print.txt(user, "c-ct-message", "c-ct-message-user"));
        ret.add(Print.txt("> ", "c-ct-message"));
        ret.add(Print.txt(message, "c-ct-message"));
        ret.add(Print.txt("\n", "c-ct-message"));
        return ret;
    }
}
