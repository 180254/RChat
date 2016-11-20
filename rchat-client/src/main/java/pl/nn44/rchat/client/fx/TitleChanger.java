package pl.nn44.rchat.client.fx;

import com.google.common.base.Strings;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class TitleChanger implements Consumer<String> {

    public static final String PREFIX = "RChat";

    private final Stage primaryStage;

    public TitleChanger(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void accept(String s) {
        String title = Strings.isNullOrEmpty(s)
                ? PREFIX
                : PREFIX + " - " + s;

        primaryStage.setTitle(title);
    }
}
