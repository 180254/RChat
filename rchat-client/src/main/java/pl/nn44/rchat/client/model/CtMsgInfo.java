package pl.nn44.rchat.client.model;

import javafx.scene.text.Text;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class CtMsgInfo {

    private final LocaleHelper i18n;
    private final LocalDateTime time;
    private final String resKey;
    private final Object[] arguments;

    public CtMsgInfo(LocaleHelper locHelper,
                     LocalDateTime time,
                     String resKey, Object... resArgs) {

        this.i18n = locHelper;
        this.time = time;
        this.resKey = resKey;
        this.arguments = resArgs.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public List<Text> toNodes() {
        String info = i18n.get(resKey, arguments);
        String info2 = MessageFormat.format("{0} {1}\n", CtMsg.time(time), info);

        Text text = CtMsg.txt(info2, "c-ct-message", "c-ct-message-info");
        return Collections.singletonList(text);
    }
}
