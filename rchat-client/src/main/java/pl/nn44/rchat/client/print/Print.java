package pl.nn44.rchat.client.print;

import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Print {

    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:MM:SS");

    public static Text txt(String value, String... classes) {
        Text text = new Text(value);
        text.getStyleClass().addAll(classes);
        return text;
    }

    public static String time(LocalDateTime time) {
        return dtf.format(time);
    }
}
