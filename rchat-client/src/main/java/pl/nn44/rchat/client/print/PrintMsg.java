package pl.nn44.rchat.client.print;

import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrintMsg implements Printable {

    private final LocalDateTime time;
    private final String user;
    private final String message;

    // ---------------------------------------------------------------------------------------------------------------

    public PrintMsg(LocalDateTime time, String user, String message) {
        this.time = time;
        this.user = user;
        this.message = message;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public List<Text> toNodes() {
        List<Text> ret = new ArrayList<>();
        ret.add(PrintUtil.txt(PrintUtil.time(time), "c-ct-message"));
        ret.add(PrintUtil.txt(" <", "c-ct-message"));
        ret.add(PrintUtil.txt(user, "c-ct-message", "c-ct-message-user"));
        ret.add(PrintUtil.txt("> ", "c-ct-message"));
        ret.add(PrintUtil.txt(message, "c-ct-message"));
        ret.add(PrintUtil.txt("\n", "c-ct-message"));
        return ret;
    }
}
