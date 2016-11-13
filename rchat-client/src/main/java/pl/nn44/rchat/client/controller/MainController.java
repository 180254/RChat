package pl.nn44.rchat.client.controller;

import com.google.common.collect.ImmutableMap;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.RefreshableListViewSkin;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.model.CtChannel;
import pl.nn44.rchat.client.model.CtMessage;
import pl.nn44.rchat.client.model.CtUser;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.RcChannel;
import pl.nn44.rchat.protocol.WhatsUp;
import pl.nn44.rchat.protocol.WhatsUp.What;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    @FXML public TextFlow messages;
    @FXML public ScrollPane scroll;
    @FXML public TextField message;
    @FXML public Button send;
    @FXML public TextField topic;
    @FXML public ListView<CtChannel> channels;
    @FXML public ListView<CtUser> users;

    public RefreshableListViewSkin<CtChannel> channelsSkin;
    public RefreshableListViewSkin<CtUser> usersSkin;

    private boolean fatalFail =
            false;

    private Map<String, CtChannel> channelsMap =
            new HashMap<>();

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

    private ObservableList<Text> messageSource =
            FXCollections.emptyObservableList();

    private ListChangeListener<Text> takeMassage =
            new ListChangeListener<Text>() {
                @Override
                public void onChanged(Change<? extends Text> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            runLater(() -> messages.getChildren().addAll(c.getAddedSubList()));
                        } else {
                            throw new AssertionError("takeMassage #0: " + c.toString());
                        }
                    }
                }
            };

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
        initMessagesScrollListener();

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
                    CtChannel ctChannel = new CtChannel(rcChannel);
                    this.channels.getItems().add(ctChannel);
                    channelsMap.put(rcChannel.getName(), ctChannel);
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

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoMsg = whatsUp.getParams()[1];
        String someText = whatsUp.getParams()[2];

        CtChannel ctChannel = channelsMap.get(channel);
        CtMessage ctMessage = new CtMessage(whoMsg, time, someText);
        ctChannel.getMessages().addAll(ctMessage.toNodes());
    }

    public void onSomePrivy(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePrivy", whatsUp);

        String whoMsgTo = whatsUp.getParams()[0];
        String whoMsgBy = whatsUp.getParams()[0];
        String someText = whatsUp.getParams()[2];
    }

    public void onSomeTopic(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeTopic", whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoChanged = whatsUp.getParams()[0];
        String someText = whatsUp.getParams()[2];

        CtChannel ctChannel = channelsMap.get(channel);

        String info = i18n.get("whats-up.TOPIC", channel, whoChanged, someText);
        Text text = CtMessage.text(info, "c-ct-message-info");

        ctChannel.setTopic(someText);
        ctChannel.getMessages().addAll(text);
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
                (observable, oldValue, newValue)
                        -> onSingleClickedChannels(newValue)
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

        messageSource.removeListener(takeMassage);
        messageSource = channel.getMessages();
        messages.getChildren().setAll(messageSource);
        messageSource.addListener(takeMassage);

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
                        messages.getChildren().clear();
                        onSingleClickedChannels(channel);
                    });

                } catch (Exception e) {
                    submitFleetingStatus(r(i18n.mapError("part", e)));
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void initMessagesScrollListener() {
        messages.heightProperty().addListener((observable, oldValue, newValue) -> {
            boolean oneLineAdded = newValue.longValue() - oldValue.longValue() < 15L;
            boolean topScroll = scroll.getVvalue() < 0.1;

            if (oneLineAdded || topScroll) {
                scroll.setVvalue(1.0);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onKeyMessagePressed(KeyEvent ev) {
        CtChannel channel = channels.getSelectionModel().getSelectedItem();

        if (ev.getCode() == KeyCode.ENTER) {
            onSendAction(null);
        }

        if (channel != null) {
            channel.setCurrentMsg(message.getText());
        }

    }

    @FXML
    public void onSendAction(ActionEvent ev) {
        if (send.isDisabled()) {
            return;
        }

        String message = this.message.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        this.message.setText("");

        CtChannel channel = this.channels.getSelectionModel().getSelectedItem();

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
            }

            runLater(() -> status.setText(""));
        });
    }
}
