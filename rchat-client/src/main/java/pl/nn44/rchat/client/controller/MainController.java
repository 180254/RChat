package pl.nn44.rchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.protocol.ChannelUser;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final CsHandler csHandler;

    @FXML public Label status;
    @FXML public TextFlow text;
    @FXML public TextField message;
    @FXML public Button send;
    @FXML public ListView<Object> channels;
    @FXML public ListView<ChannelUser> users;

    public MainController(CsHandler csHandler) {
        this.csHandler = csHandler;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> message.requestFocus());

        users.setCellFactory(param -> new ListCell<ChannelUser>() {
            @Override
            protected void updateItem(ChannelUser item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    return;
                }

                String modes = "";
                modes += item.isAuthorized() ? "a" : "";
                modes += item.isAdmin() ? "o" : "";
                modes += item.isIgnored() ? "i" : "";
                modes = !modes.isEmpty() ? " (" + modes + ")" : "";

                setText(item.getUsername() + modes);
            }
        });

        users.setOnContextMenuRequested(event -> new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                System.out.println("xx");
            }
        });

        channels.getItems().add("any channel");
        channels.getItems().add("other channel");
        channels.getItems().add("other 123");

        users.getItems().add(new ChannelUser("x", "any_user", true, true, true));
        users.getItems().add(new ChannelUser("x", "other_user", true, true, false));
        users.getItems().add(new ChannelUser("x", "other_123", true, false, true));
        users.getItems().add(new ChannelUser("x", "some", false, false, false));
    }

    @FXML
    public void sendClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
        channels.getSelectionModel().select(1);
    }
}
