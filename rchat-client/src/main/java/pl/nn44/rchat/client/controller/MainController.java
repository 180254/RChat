package pl.nn44.rchat.client.controller;

import com.google.common.collect.ImmutableMap;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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
import pl.nn44.rchat.client.model.CtMsgInfo;
import pl.nn44.rchat.client.model.CtMsgUser;
import pl.nn44.rchat.client.model.CtUser;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.*;
import pl.nn44.rchat.protocol.WhatsUp.What;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final Pattern NL_PATTERN = Pattern.compile("\r?\n");
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
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

    private final Map<What, Consumer<WhatsUp>> whatsUpMap =
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

    private final Map<String, BiConsumer<String, String[]>> commandsMap =
            ImmutableMap.<String, BiConsumer<String, String[]>>builder()
                    .put("/join", this::onCmdJoin)
                    .put("/part", this::onCmdPart)
                    .put("/topic", this::onCmdTopic)
                    .put("/kick", this::onCmdKick)
                    .put("/ban", this::onCmdBan)
                    .put("/admin", this::onCmdAdmin)
                    .put("/ignore", this::onCmdIgnore)
                    .build();

    // ---------------------------------------------------------------------------------------------------------------

    private Map<String, CtChannel> channelsMap =
            new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------


    private ObservableList<CtUser> usersModel;

    private ObservableList<CtUser> usersSource =
            FXCollections.emptyObservableList();

    private final ListChangeListener<CtUser> usersTake =
            new ListChangeListener<CtUser>() {
                @Override
                public void onChanged(Change<? extends CtUser> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            runLater(() -> usersModel.addAll(c.getAddedSubList()));
                        } else if (c.wasRemoved()) {
                            runLater(() -> usersModel.removeAll(c.getRemoved()));
                        } else {
                            throw new AssertionError("usersTake #0: " + c.toString());
                        }
                    }
                }
            };

    // ---------------------------------------------------------------------------------------------------------------

    private ObservableList<Node> messagesModel;

    private ObservableList<Text> messagesSource =
            FXCollections.emptyObservableList();

    private final ListChangeListener<Text> messageTake =
            new ListChangeListener<Text>() {
                @Override
                public void onChanged(Change<? extends Text> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            runLater(() -> messagesModel.addAll(c.getAddedSubList()));
                        } else if (c.wasRemoved()) {
                            runLater(() -> messagesModel.removeAll(c.getRemoved()));
                        } else {
                            throw new AssertionError("messageTake #0: " + c.toString());
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

        usersModel = users.getItems();
        messagesModel = messages.getChildren();

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

                // channel cannot be removed or added dynamically
                channelsMap = Collections.unmodifiableMap(channelsMap);

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

        CtMsgUser ctMsgUser = new CtMsgUser(whoMsg, time, someText);

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getMessages().addAll(ctMsgUser.toNodes());
    }

    public void onSomePrivy(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePrivy", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String whoMsgTo = whatsUp.getParams()[0];
        String whoMsgBy = whatsUp.getParams()[0];
        String someText = whatsUp.getParams()[2];

        LOG.info("?", time, whoMsgTo, whoMsgBy, someText);
    }

    public void onSomeTopic(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeTopic", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoChanged = whatsUp.getParams()[1];
        String someText = whatsUp.getParams()[2];

        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.topic", channel, whoChanged, someText
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.setTopic(someText);
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    public void onSomeJoin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeJoin", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoJoined = whatsUp.getParams()[1];
        boolean auth = Boolean.parseBoolean(whatsUp.getParams()[2]);
        boolean admin = Boolean.parseBoolean(whatsUp.getParams()[3]);

        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.join", channel, whoJoined
        );

        RcChUser rcChUser = new RcChUser(channel, whoJoined, auth, false, admin, false);
        CtUser ctUser = new CtUser(rcChUser);

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().add(ctUser);
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    public void onSomePart(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePart", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoPart = whatsUp.getParams()[1];

        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.part", channel, whoPart
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(whoPart));
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    public void onSomeKick(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePart", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoKicked = whatsUp.getParams()[1];
        String whoKickedBy = whatsUp.getParams()[1];

        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.kick", channel, whoKicked, whoKickedBy
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(whoKicked));
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    public void onSomeBan(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeBan", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoBanned = whatsUp.getParams()[1];
        String whoBannedBy = whatsUp.getParams()[2];
        boolean state = whatsUp.getParams()[3].equals("ON");

        String code = state ? "1" : "0";
        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.ban." + code, channel, whoBanned, whoBannedBy
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(whoBanned))
                .forEach(u -> u.setBanned(state));
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeAdmin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeBan", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoAdmin = whatsUp.getParams()[1];
        String whoAdminBy = whatsUp.getParams()[2];
        boolean state = whatsUp.getParams()[3].equals("ON");

        String code = state ? "1" : "0";
        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.admin." + code, channel, whoAdmin, whoAdminBy
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().stream().
                filter(u -> u.getUsername().equals(whoAdmin))
                .forEach(u -> u.setAdmin(state));
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeIgnore(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeIgnore", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoIgnored = whatsUp.getParams()[1];
        String whoIgnoredBy = whatsUp.getParams()[2];
        boolean state = whatsUp.getParams()[3].equals("ON");

        String code = state ? "1" : "0";
        CtMsgInfo ctMsgInfo = new CtMsgInfo(
                i18n, time, "whats-up.ignore." + code, channel, whoIgnored, whoIgnoredBy
        );

        CtChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(whoIgnored))
                .forEach(u -> u.setIgnored(state));
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());

        runLater(() -> usersSkin.refresh());
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
        topic.setText(channel.getTopic());

        if (messagesSource != channel.getMessages()) {
            messagesSource.removeListener(messageTake);
            messagesSource = channel.getMessages();
            messagesModel.setAll(messagesSource);
            messagesSource.addListener(messageTake);
        }

        if (usersSource != channel.getUsers()) {
            usersSource.removeListener(usersTake);
            usersSource = channel.getUsers();
            usersModel.setAll(usersSource);
            usersSource.addListener(usersTake);
        }

        usersSkin.refresh();
        channelsSkin.refresh();
    }

    public void onDoubleClickedChannels(CtChannel channel) {
        if (!channel.isJoin()) {
            // join
            exs.submit(() -> {
                try {
                    RcChannel rcChannel =
                            csh.cs().join(csh.token(), channel.getName(), null).getPayload();

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
                    csh.cs().part(csh.token(), channel.getName(), "unused");

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

        if (message.startsWith("/")) {
            exs.submit(() -> onAnyCmd(channel.getName(), message));

        } else {
            exs.submit(() -> {
                try {
                    csh.cs().message(csh.token(), channel.getName(), message);
                } catch (ChatException e) {
                    submitFleetingStatus(r(i18n.mapError("message", e)));
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onAnyCmd(String channel, String message) {
        String[] tokens = message.split(" ");

        BiConsumer<String, String[]> cmdConsumer = commandsMap.get(tokens[0]);

        if (cmdConsumer == null) {
            submitFleetingStatus(r(i18n.get("cmd.unknown")));
        } else {
            cmdConsumer.accept(channel, tokens);
        }
    }

    public void onSimpleCommand(String channel, String[] tokens, String locMap, SimpleCmd sc) {
        if (tokens.length < 2) {
            submitFleetingStatus(r(i18n.get("cmd." + locMap + ".syntax")));
            return;
        }

        try {
            String param = Stream.of(tokens).skip(1).collect(Collectors.joining(" "));
            sc.accept(csh.token(), channel, param);

        } catch (Exception e) {
            submitFleetingStatus(r(i18n.mapError(locMap, e)));
        }
    }

    public void onStateCommand(String channel, String[] tokens, String locMap, StatefulCmd sc) {
        if (tokens.length < 3) {
            submitFleetingStatus(r(i18n.get("cmd." + locMap + ".syntax")));
            return;
        }

        if (!(tokens[2].equals("on") || tokens[2].equals("off"))) {
            submitFleetingStatus(r(i18n.get("cmd." + locMap + ".syntax")));
            return;
        }

        try {
            sc.accept(csh.token(), channel, tokens[1], tokens[2].equals("on"));
        } catch (Exception e) {
            submitFleetingStatus(r(i18n.mapError(locMap, e)));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onCmdJoin(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdJoin", channel, tokens);
    }

    public void onCmdPart(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdPart", channel, tokens);
    }

    public void onCmdKick(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdKick", channel, tokens);

        ChatService cs = csh.cs();
        onSimpleCommand(channel, tokens, "kick", cs::kick);
    }

    public void onCmdBan(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdBan", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "ban", cs::ban);
    }

    public void onCmdAdmin(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdAdmin", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "admin", cs::admin);
    }

    public void onCmdIgnore(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdIgnore", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "ignore", cs::ignore);
    }

    public void onCmdTopic(String channel, String tokens[]) {
        LOG.info("{} {} {}", "onCmdTopic", channel, tokens);

        ChatService cs = csh.cs();
        onSimpleCommand(channel, tokens, "topic", cs::topic);
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
                LOG.debug("submitFleetingStatus: InterruptedException");
            }

            runLater(() -> status.setText(""));
        });
    }
}
