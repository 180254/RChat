package pl.nn44.rchat.client.model;

import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CtMsgUser {

    private final String user;
    private final LocalDateTime time;
    private final String message;

    public CtMsgUser(String user, LocalDateTime time, String message) {
        this.user = user;
        this.time = time;
        this.message = message;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public List<Text> toNodes() {
        List<Text> ret = new ArrayList<>();
        ret.add(CtMsg.txt(CtMsg.time(time), "c-ct-message"));
        ret.add(CtMsg.txt(" <", "c-ct-message"));
        ret.add(CtMsg.txt(user, "c-ct-message", "c-ct-message-user"));
        ret.add(CtMsg.txt("> ", "c-ct-message"));
        ret.add(CtMsg.txt(message, "c-ct-message"));
        ret.add(CtMsg.txt("\n", "c-ct-message"));
        return ret;
    }
}
