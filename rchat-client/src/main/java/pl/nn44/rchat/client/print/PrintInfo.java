package pl.nn44.rchat.client.print;

import javafx.scene.Node;
import javafx.scene.text.Text;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class PrintInfo implements Printable {

    private final LocaleHelper i18n;
    private final LocalDateTime time;
    private final String resKey;
    private final Object[] arguments;

    public PrintInfo(LocaleHelper locHelper,
                     LocalDateTime time,
                     String resKey, Object... resArgs) {

        this.i18n = locHelper;
        this.time = time;
        this.resKey = resKey;
        this.arguments = resArgs.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public List<Node> toNodes() {
        String info = i18n.get(resKey, arguments);
        String info2 = MessageFormat.format("{0} {1}\n", Print.time(time), info);

        Text text = Print.txt(info2, "c-ct-message", "c-ct-message-info");
        return Collections.singletonList(text);
    }
}
