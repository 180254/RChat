package pl.nn44.rchat.client.fx;

import com.google.common.base.Strings;
import javafx.stage.Stage;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.util.function.Consumer;

public class TitleChanger implements Consumer<String> {

    private final Stage stage;
    private final LocaleHelper i18n;

    public TitleChanger(Stage primaryStage, LocaleHelper locHelper) {
        this.stage = primaryStage;
        this.i18n = locHelper;
    }

    @Override
    public void accept(String s) {
        String title = Strings.isNullOrEmpty(s)
                ? i18n.get("title.alone")
                : i18n.get("title.string", s);

        stage.setTitle(title);
    }
}
