package pl.nn44.rchat.client.controller;

import com.google.common.collect.ImmutableMap;
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
import pl.nn44.rchat.client.util.Lf;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.RcChannel;
import pl.nn44.rchat.protocol.Response;
import pl.nn44.rchat.protocol.WhatsUp;
import pl.nn44.rchat.protocol.WhatsUp.What;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final int WHATS_UP_LONG_POOLING = (int) TimeUnit.MINUTES.toMillis(1);

    private final ExecutorService exs;
    private final CsHandler csh;
    private final LocaleHelper i18n;

    @FXML public Label status;
    @FXML public TextFlow text;
    @FXML public TextField message;
    @FXML public Button send;
    @FXML public TextField topic;
    @FXML public ListView<CtChannel> channels;
    @FXML public ListView<CtUser> users;

    public RefreshableListViewSkin<CtChannel> channelsSkin;
    public RefreshableListViewSkin<CtUser> usersSkin;

    private Map<What, Consumer<WhatsUp>> whatsUpMap =
            ImmutableMap.<What, Consumer<WhatsUp>>builder()
                    .put(What.MESSAGE, this::onSomeMessage)
                    .put(What.PRIVY, this::onSomePrivy)
                    .put(What.TOPIC, this::onSomeTopic)
                    .put(What.JOIN, this::onSomeJoin)
                    .put(What.PART, this::onSomePart)
                    .put(What.KICK, this::onSomeKick)
                    .put(What.BAN, this::onSomeBan)
                    .put(What.ADMIN, this::onSomeAdmin)
                    .put(What.IGNORE, this::onSomeIgnore)
                    .build();

    // ---------------------------------------------------------------------------------------------------------------

    public MainController(ExecutorService executor, CsHandler csHandler,
                          LocaleHelper localeHelper) {

        this.csh = csHandler;
        this.exs = executor;
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

        exs.submit(() -> {
            runLater(() -> {
                status.setText(Lf.r(i18n.get("ctrl.main.initializing")));
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

                exs.submit(this::listenWhatHappens);
            } catch (Exception e) {
                runLater(() -> status.setText(Lf.r(i18n.mapError("channels", e))));
            }
        });

    }

    // ---------------------------------------------------------------------------------------------------------------

    public void listenWhatHappens() {

        try {
            WhatsUp[] whatsUps = csh.cs()
                    .whatsUp(csh.token(), WHATS_UP_LONG_POOLING)
                    .getPayload();

            for (WhatsUp whatsUp : whatsUps) {
                whatsUpMap.get(whatsUp.getWhat()).accept(whatsUp);
            }

            exs.submit(this::listenWhatHappens);
        } catch (Exception e) {
            LOG.error("XX", e);
        }

    }

    public void onSomeMessage(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeMessage", whatsUp);
    }

    public void onSomePrivy(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePrivy", whatsUp);
    }

    public void onSomeTopic(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeTopic", whatsUp);
    }

    public void onSomeJoin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeJoin", whatsUp);
    }

    public void onSomePart(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePart", whatsUp);
    }

    public void onSomeKick(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeKick", whatsUp);
    }

    public void onSomeBan(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeBan", whatsUp);
    }

    public void onSomeAdmin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeAdmin", whatsUp);
    }

    public void onSomeIgnore(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeIgnore", whatsUp);
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
            exs.submit(() -> {
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
                    runLater(() -> status.setText(Lf.r(i18n.mapError("join", e))));
                }
            });

        } else {
            exs.submit(() -> {
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
                    runLater(() -> status.setText(Lf.r(i18n.mapError("part", e))));
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
