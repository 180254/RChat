package pl.nn44.rchat.client.controller;

import com.google.common.collect.ImmutableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.RefreshableListViewSkin;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.model.CtChannel;
import pl.nn44.rchat.client.model.CtUser;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.RcChannel;
import pl.nn44.rchat.protocol.WhatsUp;
import pl.nn44.rchat.protocol.WhatsUp.What;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static javafx.application.Platform.runLater;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final Pattern NL_PATTERN = Pattern.compile("\r?\n");
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

    private boolean fatalFail = false;

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

    public MainController(ExecutorService executor,
                          CsHandler csHandler,
                          LocaleHelper localeHelper) {

        this.csh = csHandler;
        this.exs = executor;
        this.i18n = localeHelper;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        channelsSkin = new RefreshableListViewSkin<>(channels);
        usersSkin = new RefreshableListViewSkin<>(users);

        initChannelChangeListener();

        exs.submit(() -> {
            runLater(() -> {
                message.requestFocus();
                status.setText(r(i18n.get("ctrl.main.initializing")));
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
                fatalFail = true;
                runLater(() -> status.setText(r(i18n.mapError("channels", e))));
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

        } catch (RejectedExecutionException e) {
            LOG.debug("listenWhatHappens: RejectedExecutionException.");

        } catch (Exception e) {
            fatalFail = true;

            runLater(() -> {
                status.setText(r(i18n.mapError("whats-up", e)));
                send.setDisable(true);
            });
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

    public void initChannelChangeListener() {
        channels.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onSingleClickedChannels(newValue)
        );
    }

    @FXML
    public void onMouseClickedChannels(MouseEvent ev) {
        CtChannel channel = channels.getSelectionModel().getSelectedItem();

        if (channel == null) {
            return;
        }

        // maybe clicked empty field?
        if (ev.getPickResult().getIntersectedNode() instanceof ListCell) {
            return;
        }

        if (ev.getClickCount() % 2 == 0) {
            onDoubleClickedChannels(channel);
        }
    }

    @FXML
    public void onKeyPressedChannels(KeyEvent ev) {
        CtChannel channel = channels.getSelectionModel().getSelectedItem();

        if (channel == null) {
            return;
        }

        if ("\r\n ".contains(ev.getCharacter())) {
            onDoubleClickedChannels(channel);
        }
    }


    public void onSingleClickedChannels(CtChannel channel) {
        send.setDisable(!fatalFail && !(channel.isJoin()));

        message.setText(channel.getCurrentMsg());

        topic.setText(channel.getTopic());
        users.setItems(channel.getUsers());

        usersSkin.refresh();
        channelsSkin.refresh();
    }

    public void onDoubleClickedChannels(CtChannel channel) {
        if (!channel.isJoin()) {
            // join
            exs.submit(() -> {
                try {
                    RcChannel rcChannel = csh.cs().join(
                            csh.token(),
                            channel.getName(),
                            null
                    ).getPayload();

                    channel.setJoin(true);

                    runLater(() -> {
                        channel.update(rcChannel);
                        onSingleClickedChannels(channel);
                    });

                } catch (Exception e) {
                    submitFleetingStatus(r(i18n.mapError("join", e)));
                }
            });

        } else {
            // part
            exs.submit(() -> {
                try {
                    csh.cs().part(
                            csh.token(),
                            channel.getName()
                    );

                    channel.setJoin(false);

                    runLater(() -> {
                        channel.clear();
                        onSingleClickedChannels(channel);
                    });

                } catch (Exception e) {
                    submitFleetingStatus(r(i18n.mapError("part", e)));
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onKeyPressed(KeyEvent ev) {
        CtChannel channel = channels.getSelectionModel().getSelectedItem();

        if (channel != null) {
            channel.setCurrentMsg(message.getText());
        }
    }

    @FXML
    public void sendClicked(ActionEvent ev) {
        String message = this.message.getText();
        CtChannel channel = this.channels.getSelectionModel().getSelectedItem();
        this.message.setText("");

        exs.submit(() -> {
            try {
                csh.cs().message(csh.token(), channel.getName(), message);
            } catch (ChatException e) {
                submitFleetingStatus(r(i18n.mapError("message", e)));
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    private String r(String text) {
        return NL_PATTERN.matcher(text).replaceAll(" ");
    }

    private void submitFleetingStatus(String text) {
        exs.submit(() -> {
            runLater(() -> status.setText(text));

            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {
                LOG.warn("fleetingStatus interrupted", e);
                throw new AssertionError(e);
            }

            runLater(() -> status.setText(""));
        });
    }
}
