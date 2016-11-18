package pl.nn44.rchat.client.print;

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
    private final Object[] resArgs;

    // ---------------------------------------------------------------------------------------------------------------

    public PrintInfo(LocaleHelper locHelper,
                     LocalDateTime time,
                     String resKey,
                     Object[] resArgs) {

        this.i18n = locHelper;
        this.time = time;
        this.resKey = resKey;
        this.resArgs = resArgs.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public List<Text> toNodes() {
        String info = i18n.get(resKey, resArgs);
        String info2 = MessageFormat.format("{0} {1}\n", PrintUtil.time(time), info);

        Text text = PrintUtil.txt(info2, "c-ct-message", "c-ct-message-info");
        return Collections.singletonList(text);
    }
}
