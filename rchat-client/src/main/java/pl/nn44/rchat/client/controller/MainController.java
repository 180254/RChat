package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.model.ChannelEx;
import pl.nn44.rchat.client.model.UserEx;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.RChannel;
import pl.nn44.rchat.protocol.Response;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

import static java.util.concurrent.CompletableFuture.runAsync;
import static javafx.application.Platform.runLater;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final CsHandler csh;
    private final LocaleHelper i18n;

    @FXML public Label status;
    @FXML public TextFlow text;
    @FXML public TextField message;
    @FXML public Button send;
    @FXML public TextField topic;
    @FXML public ListView<ChannelEx> channels;
    @FXML public ListView<UserEx> users;

    // ---------------------------------------------------------------------------------------------------------------

    public MainController(CsHandler csHandler, LocaleHelper localeHelper) {
        this.csh = csHandler;
        i18n = localeHelper;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runLater(() -> message.requestFocus());

        runAsync(() -> {
            runLater(() -> {
                status.setText(i18n.get("ctrl.main.initializing"));
                send.setDisable(true);
            });

            try {
                Response<RChannel[]> channels = csh.cs().channels(csh.token());
                for (RChannel rChannel : channels.getPayload()) {
                    ChannelEx channelEx = new ChannelEx(rChannel);
                    this.channels.getItems().add(channelEx);
                }

                runLater(() -> {
                    status.setText("");
                });
            } catch (Exception e) {
                status.setText(i18n.mapError("main", e));
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void sendClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
        channels.getSelectionModel().select(1);
    }
}
