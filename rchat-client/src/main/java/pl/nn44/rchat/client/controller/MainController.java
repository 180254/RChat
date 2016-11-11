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
import pl.nn44.rchat.client.model.CtChannel;
import pl.nn44.rchat.client.model.CtUser;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.RcChannel;
import pl.nn44.rchat.protocol.Response;

import java.net.URL;
import java.util.List;
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
    @FXML public ListView<CtChannel> channels;
    @FXML public ListView<CtUser> users;

    private RefreshableListViewSkin<CtChannel> channelsSkin;
    private RefreshableListViewSkin<CtUser> usersSkin;

    // ---------------------------------------------------------------------------------------------------------------

    public MainController(CsHandler csHandler,
                          LocaleHelper localeHelper) {

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
                status.setText(i18n.get2("ctrl.main.initializing"));
                send.setDisable(true);
            });

            try {
                RcChannel[] channels = csh.cs()
                        .channels(csh.token())
                        .getPayload();

                for (RcChannel rcChannel : channels) {
                    CtChannel channelEx = new CtChannel(rcChannel);
                    this.channels.getItems().add(channelEx);
                }

                runLater(() -> status.setText(""));

            } catch (Exception e) {
                runLater(() -> status.setText(i18n.mapError2("channels", e)));
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onMouseClickedChannels(MouseEvent ev) {
        CtChannel selected = channels.getSelectionModel().getSelectedItem();

        if (ev.getClickCount() == 1) {
            onSingleClickedChannels(ev, selected);

        } else if (ev.getClickCount() == 2) {
            onDoubleClickedChannels(ev, selected);
        }
    }


    public void onSingleClickedChannels(MouseEvent ev, CtChannel selected) {
        System.out.println("SINGLE");
    }

    public void onDoubleClickedChannels(MouseEvent ev, CtChannel selected) {
        if (!selected.isJoin()) {
            runAsync(() -> {
                try {
                    RcChannel rcChannel = csh.cs().join(
                            csh.token(),
                            selected.getChannel().getName(),
                            null
                    ).getPayload();

                    List<CtUser> ctUsers = Stream
                            .of(rcChannel.getRcChUsers())
                            .map(CtUser::new)
                            .collect(Collectors.toList());

                    ObservableList<CtUser> oCtUsers = FXCollections.observableArrayList(ctUsers);

                    runLater(() -> {
                        selected.setJoin(true);
                        selected.setChannel(rcChannel);

                        topic.setText(rcChannel.getTopic());
                        users.setItems(oCtUsers);
                        channelsSkin.refresh();
                    });

                } catch (Exception e) {
                    runLater(() -> status.setText(i18n.mapError2("join", e)));
                }
            });

        } else {
            runAsync(() -> {
                try {
                    Response<?> part = csh.cs().part(
                            csh.token(),
                            selected.getChannel().getName()
                    );

                    runLater(() -> {
                        selected.setJoin(false);
                        users.setItems(null);
                        channelsSkin.refresh();
                    });

                } catch (Exception e) {
                    runLater(() -> status.setText(i18n.mapError2("part", e)));
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void sendClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
        channels.getSelectionModel().select(1);
    }
}
