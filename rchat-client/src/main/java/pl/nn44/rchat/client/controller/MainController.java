package pl.nn44.rchat.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.RefreshableListViewSkin;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.model.CChannel;
import pl.nn44.rchat.client.model.CUser;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.RChannel;
import pl.nn44.rchat.protocol.Response;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @FXML public ListView<CChannel> channels;
    @FXML public ListView<CUser> users;

    private RefreshableListViewSkin<CChannel> channelsSkin;
    private RefreshableListViewSkin<CUser> usersSkin;

    // ---------------------------------------------------------------------------------------------------------------

    public MainController(CsHandler csHandler, LocaleHelper localeHelper) {
        this.csh = csHandler;
        i18n = localeHelper;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        channelsSkin = new RefreshableListViewSkin<>(channels);
        channels.setSkin(channelsSkin);

        usersSkin = new RefreshableListViewSkin<>(users);
        users.setSkin(usersSkin);

        runLater(() -> message.requestFocus());

        runAsync(() -> {
            runLater(() -> {
                status.setText(i18n.get("ctrl.main.initializing"));
                send.setDisable(true);
            });

            try {
                Response<RChannel[]> channels = csh.cs().channels(csh.token());
                for (RChannel rChannel : channels.getPayload()) {
                    CChannel channelEx = new CChannel(rChannel);
                    this.channels.getItems().add(channelEx);
                }

                runLater(() -> status.setText(""));
            } catch (Exception e) {
                runLater(() -> status.setText(i18n.mapError("channels", e)));
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onMouseClickedChannels(MouseEvent ev) {
        CChannel selected = channels.getSelectionModel().getSelectedItem();

        if (ev.getClickCount() == 1) {
            onSingleClickedChannels(ev, selected);

        } else if (ev.getClickCount() == 2) {
            onDoubleClickedChannels(ev, selected);
        }
    }


    public void onSingleClickedChannels(MouseEvent ev, CChannel selected) {
        if (!selected.isJoin()) {
            runAsync(() -> {
                try {
                    Response<RChannel> rChannel = csh.cs().join(
                            csh.token(),
                            selected.getRChannel().getName(),
                            null
                    );
                    ObservableList<CUser> orChUsers = FXCollections.observableArrayList(
                            Stream.of(rChannel.getPayload().getrChUsers())
                                    .map(CUser::new)
                                    .collect(Collectors.toList())
                    );

                    channelsSkin.refresh();
                    selected.setJoin(true);
                    selected.setRChannel(rChannel.getPayload());
                    channels.getItems();
                    users.setItems(orChUsers);
                } catch (Exception e) {
                    runLater(() -> status.setText(i18n.mapError2("join", e)));
                }
            });
        } else {

        }
    }

    public void onDoubleClickedChannels(MouseEvent ev, CChannel selected) {

    }

    // ---------------------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void sendClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
        channels.getSelectionModel().select(1);
    }
}
