package pl.nn44.rchat.client.controller;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.RefreshableListViewSkin;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.model.ClientChannel;
import pl.nn44.rchat.client.model.ClientUser;
import pl.nn44.rchat.client.print.PrintInfo;
import pl.nn44.rchat.client.print.PrintMsg;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.command.SimpleCommand;
import pl.nn44.rchat.protocol.command.StatefulCommand;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Channel;
import pl.nn44.rchat.protocol.model.User;
import pl.nn44.rchat.protocol.model.WhatsUp;
import pl.nn44.rchat.protocol.model.WhatsUp.What;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
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

    public static final int WHATS_UP_LONG_POOLING = (int) TimeUnit.MINUTES.toMillis(1);

    private final ScheduledExecutorService exs;
    private final CsHandler csh;
    private final LocaleHelper i18n;
    private final Stage stage;

    @FXML public Label status;
    @FXML public TextFlow messages;
    @FXML public ScrollPane scroll;
    @FXML public TextField message;
    @FXML public Button send;
    @FXML public TextField topic;
    @FXML public ListView<ClientChannel> channels;
    @FXML public ListView<ClientUser> users;

    @FXML public MenuBar menu;
    @FXML public MenuController menuController;

    // ---------------------------------------------------------------------------------------------------------------

    public MainController(ScheduledExecutorService executor,
                          CsHandler csHandler,
                          LocaleHelper localeHelper,
                          Stage primaryStage) {

        this.csh = csHandler;
        this.exs = executor;
        this.i18n = localeHelper;
        this.stage = primaryStage;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public RefreshableListViewSkin<ClientChannel> channelsSkin; // initialize-final
    public RefreshableListViewSkin<ClientUser> usersSkin; // initialize-final

    private final Map<What, Consumer<WhatsUp>> whatsUpMap =
            ImmutableMap.<What, Consumer<WhatsUp>>builder()
                    .put(What.NOTHING, this::onSomeNothing)
                    .put(What.JOIN, this::onSomeJoin)
                    .put(What.PART, this::onSomePart)
                    .put(What.TOPIC, this::onSomeTopic)
                    .put(What.KICK, this::onSomeKick)
                    .put(What.BAN, this::onSomeBan)
                    .put(What.ADMIN, this::onSomeAdmin)
                    .put(What.IGNORE, this::onSomeIgnore)
                    .put(What.MESSAGE, this::onSomeMessage)
                    .put(What.PRIVY, this::onSomePrivy)
                    .build();

    private final Map<String, BiConsumer<String, String[]>> commandsMap =
            ImmutableMap.<String, BiConsumer<String, String[]>>builder()
                    .put("/topic", this::onCmdTopic)
                    .put("/kick", this::onCmdKick)
                    .put("/ban", this::onCmdBan)
                    .put("/admin", this::onCmdAdmin)
                    .put("/ignore", this::onCmdIgnore)
                    .build();

    // ---------------------------------------------------------------------------------------------------------------

    private final ConcurrentMap<String, ClientChannel> channelsMap =
            new ConcurrentHashMap<>();

    // ---------------------------------------------------------------------------------------------------------------

    private Property<String> topicModel; // initialize-final

    private Property<String> topicSource =
            new SimpleStringProperty();

    private final ChangeListener<String> topicTake =
            (observable, oldValue, newValue) ->
                    runLater(() -> topicModel.setValue(newValue));

    // ---------------------------------------------------------------------------------------------------------------

    private ObservableList<ClientUser> usersModel; // initialize-final

    private ObservableList<ClientUser> usersSource =
            FXCollections.emptyObservableList();

    private final ListChangeListener<ClientUser> usersTake =
            new ListChangeListener<ClientUser>() {
                @Override
                public void onChanged(Change<? extends ClientUser> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            List<ClientUser> snapshot = new ArrayList<>(c.getAddedSubList());
                            runLater(() -> usersModel.addAll(snapshot));

                        } else if (c.wasRemoved()) {
                            List<ClientUser> snapshot = new ArrayList<>(c.getRemoved());
                            runLater(() -> usersModel.removeAll(snapshot));

                        } else {
                            LOG.error("usersTake unexpected condition");
                            throw new AssertionError("usersTake #0: " + c.toString());
                        }
                    }
                }
            };

    // ---------------------------------------------------------------------------------------------------------------

    private ObservableList<Node> messagesModel; // initialize-final

    private ObservableList<Text> messagesSource =
            FXCollections.emptyObservableList();

    private final ListChangeListener<Text> messageTake =
            new ListChangeListener<Text>() {
                @Override
                public void onChanged(Change<? extends Text> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            List<Text> snapshot = new ArrayList<>(c.getAddedSubList());
                            runLater(() -> messagesModel.addAll(snapshot));

                        } else if (c.wasRemoved()) {
                            List<Text> snapshot = new ArrayList<>(c.getRemoved());
                            runLater(() -> messagesModel.removeAll(snapshot));

                        } else {
                            LOG.error("messageTake unexpected condition");
                            throw new AssertionError("messageTake #0: " + c.toString());
                        }
                    }
                }
            };

    // ---------------------------------------------------------------------------------------------------------------

    private boolean fatalFail = false;
    private final Semaphore joinPartSemaphore = new Semaphore(1);
    private Future<?> newsFuture = new FutureTask<>(Object::new);

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        channelsSkin = new RefreshableListViewSkin<>(channels);
        usersSkin = new RefreshableListViewSkin<>(users);

        topicModel = topic.textProperty();
        usersModel = FXCollections.synchronizedObservableList(users.getItems());
        messagesModel = FXCollections.synchronizedObservableList(messages.getChildren());

        menuController.beforeLogout.add(() -> newsFuture.cancel(true));

        exs.submit(() -> {
            initChannelCellFactory();
            initChannelChangeListener();
            initMessagesScrollListener();

            String initializingStatus = r(i18n.get("ctrl.main.initializing"));
            runLater(() -> {
                message.requestFocus();
                status.setText(initializingStatus);
                send.setDisable(true);
            });

            try {
                Channel[] pChannels = csh.cs()
                        .channels(csh.token())
                        .getPayload();

                for (Channel channel : pChannels) {
                    ClientChannel ctChannel = new ClientChannel(channel);
                    channelsMap.put(channel.getName(), ctChannel);
                }

                runLater(() -> {
                    channels.getItems().addAll(channelsMap.values());
                    fleetingStatusAsync(r(i18n.get("hello.motd")));
                });

                newsFuture = exs.submit(this::listenWhatHappens);

            } catch (Exception e) {
                fatalFail = true;

                String failStatus = r(i18n.mapError("channels", e));
                runLater(() -> status.setText(failStatus));
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void initChannelCellFactory() {
        channels.setCellFactory(lv -> new ListCell<ClientChannel>() {
            @Override
            protected void updateItem(ClientChannel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    return;
                }

                setText(item.toString());

                ObservableList<String> styleClass = getStyleClass();

                if (item.isUnread() && !styleClass.contains("ch-has-unread-msg")) {
                    styleClass.add("ch-has-unread-msg");
                }

                if (!item.isUnread() && styleClass.contains("ch-has-unread-msg")) {
                    styleClass.remove("ch-has-unread-msg");
                }
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void listenWhatHappens() {
        try {
            WhatsUp[] whatsUps = csh.cs()
                    .whatsUp(csh.token(), WHATS_UP_LONG_POOLING)
                    .getPayload();

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            for (WhatsUp whatsUp : whatsUps) {
                whatsUpMap.get(whatsUp.getWhat()).accept(whatsUp);
            }

            newsFuture = exs.submit(this::listenWhatHappens);

        } catch (RejectedExecutionException e) {
            LOG.debug("listenWhatHappens: RejectedExecutionException.");

        } catch (InterruptedException e) {
            LOG.debug("listenWhatHappens: InterruptedException.");

        } catch (Exception e) {
            fatalFail = true;

            String failStatus = r(i18n.mapError("whats-up", e));
            runLater(() -> {
                status.setText(failStatus);
                send.setDisable(true);
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeNothing(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeNothing", whatsUp);
    }

    // ---------------------------------------------------------------------------------------------------------------

    // stateful = has on/off af 4th para
    public void infoAboutMessage(WhatsUp whatsUp, boolean stateful) {
        LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String channel = params[0];

        String resKey = "whats-up." + whatsUp.getWhat().name().toLowerCase();
        if (stateful) {
            String pState = params[3];
            resKey += "." + pState;
        }

        PrintInfo ctMsgInfo = new PrintInfo(i18n, time, resKey, params);

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeJoin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeJoin", whatsUp);
        infoAboutMessage(whatsUp, false);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoJoined = params[1];
        boolean pAuth = Boolean.parseBoolean(params[2]);
        boolean pIgnored = Boolean.parseBoolean(params[3]);
        boolean pAdmin = Boolean.parseBoolean(params[4]);

        User user = new User(pChannel, pWhoJoined, pAuth, pIgnored, pAdmin, false);
        ClientUser clientUser = new ClientUser(user);

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ctChannel.getUsers().add(clientUser);
    }

    public void onSomePart(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePart", whatsUp);
        infoAboutMessage(whatsUp, false);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoPart = params[1];

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(pWhoPart));
    }

    public void onSomeTopic(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeTopic", whatsUp);
        infoAboutMessage(whatsUp, false);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String channel = params[0];
        String someText = params[2];

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.setTopic(someText);
    }

// ---------------------------------------------------------------------------------------------------------------

    public void onSomeKick(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeKick", whatsUp);
        infoAboutMessage(whatsUp, false);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoKicked = params[1];
        // String pWhoKickedBy = params[2];

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(pWhoKicked));

        // special case: i was kicked
        // kicked by admin - server removed me from pChannel
        if (pWhoKicked.equals(csh.getUsername())) {
            onDoubleClickedChannels(ctChannel);
        }
    }

    public void onSomeBan(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeBan", whatsUp);
        infoAboutMessage(whatsUp, true);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoBanned = params[1];
        // String pWhoBannedBy = params[2];
        String pState = params[3];
        boolean state = pState.equals("on");

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(pWhoBanned))
                .forEach(u -> u.setBanned(state));

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeAdmin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeAdmin", whatsUp);
        infoAboutMessage(whatsUp, true);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoAdmin = params[1];
        // String pWhoAdminBy = params[2];
        String pState = params[3];
        boolean state = pState.equals("on");

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(pWhoAdmin))
                .forEach(u -> u.setAdmin(state));

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeIgnore(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeIgnore", whatsUp);

        // LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        // String pChannel = params[0];
        String pWhoIgnored = params[1];
        // String pWhoIgnoredBy = params[2];
        String pState = params[3];
        boolean state = pState.equals("on");

        ClientChannel ctChannel = channels.getSelectionModel().getSelectedItem();
        if (ctChannel != null) {
            // replace param-channel with current channel
            // in protocol, for this WhatsUp, channel pos is unused
            String[] newParams = params.clone();
            newParams[0] = ctChannel.getName();

            WhatsUp wuFix = WhatsUp.create(whatsUp.getWhat(), newParams);
            infoAboutMessage(wuFix, true);
        }

        // if i ignored (not i am ignored) set ignored flag
        if (!pWhoIgnored.equals(csh.getUsername())) {
            channelsMap.values().stream()
                    .flatMap(c -> c.getUsers().stream())
                    .filter(u -> u.getUsername().equals(pWhoIgnored))
                    .forEach(u -> u.setIgnored(state));

            runLater(() -> usersSkin.refresh());
        }
    }

// ---------------------------------------------------------------------------------------------------------------

    public void onSomeMessage(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeMessage", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pChannel = params[0];
        String pWhoMsg = params[1];
        String pSomeText = params[2];

        ClientChannel ctChannel = channelsMap.get(pChannel);
        ClientChannel ctCurrent = channels.getSelectionModel().getSelectedItem();

        if (!ctCurrent.equals(ctChannel)) {
            ctChannel.setUnread(true);
            runLater(() -> channelsSkin.refresh());
        }

        PrintMsg printMsg = new PrintMsg(time, pWhoMsg, pSomeText);
        ctChannel.getMessages().addAll(printMsg.toNodes());
    }

    public void onSomePrivy(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePrivy", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String[] params = whatsUp.getParams();
        String pWhoMsgTo = params[0];
        String pWhoMsgBy = params[0];
        String pSomeText = params[2];

        LOG.info("?", time, pWhoMsgTo, pWhoMsgBy, pSomeText);
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
        ClientChannel channel = channels.getSelectionModel().getSelectedItem();

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
        ClientChannel channel = channels.getSelectionModel().getSelectedItem();

        if (channel == null) {
            return;
        }

        if ("\r\n ".contains(ev.getCharacter())) {
            onDoubleClickedChannels(channel);
        }
    }

    public void onSingleClickedChannels(ClientChannel channel) {
        channel.setUnread(false);

        boolean sendDisable = fatalFail || !channel.isJoin();
        String sendCache = channel.getSendCache();
        runLater(() -> {
            send.setDisable(sendDisable);
            message.setText(sendCache);
        });

        if (topicSource != channel.getTopic()) {
            topicSource.removeListener(topicTake);
            topicSource = channel.getTopic();
            String snapshot = topicSource.getValue();
            runLater(() -> topicModel.setValue(snapshot));
            topicSource.addListener(topicTake);
        }

        if (messagesSource != channel.getMessages()) {
            messagesSource.removeListener(messageTake);
            messagesSource = channel.getMessages();
            ArrayList<Text> snapshot = new ArrayList<>(messagesSource);
            runLater(() -> messagesModel.setAll(snapshot));
            messagesSource.addListener(messageTake);
        }

        if (usersSource != channel.getUsers()) {
            messages.getChildren();
            usersSource.removeListener(usersTake);
            usersSource = channel.getUsers();
            ArrayList<ClientUser> snapshot = new ArrayList<>(usersSource);
            runLater(() -> usersModel.setAll(snapshot));
            usersSource.addListener(usersTake);
        }

        runLater(() -> {
            usersSkin.refresh();
            channelsSkin.refresh();
        });
    }


    public void onDoubleClickedChannels(ClientChannel channel) {
        if (fatalFail) {
            return;
        }

        try {
            joinPartSemaphore.acquire();
        } catch (InterruptedException e) {
            LOG.warn("joinPartSemaphore acquire interrupted", e);
            return;
        }

        if (!channel.isJoin()) {
            // join
            Optional<String> oPassword = Optional.empty();
            if (channel.isPassword()) {
                TextInputDialog dlg = new TextInputDialog();
                dlg.initOwner(stage);
                dlg.setTitle(i18n.get("join-pass.title"));
                dlg.setHeaderText(i18n.get("join-pass.header", channel.getName()));
                dlg.getDialogPane().setContentText(i18n.get("join-pass.label"));
                dlg.setGraphic(null);
                oPassword = dlg.showAndWait();
            }

            String password = Strings.emptyToNull(oPassword.orElse(null));

            exs.submit(() -> {
                try {
                    if (channel.isPassword() && password == null) {
                        return;
                    }

                    Channel pChannel = csh.cs()
                            .join(csh.token(), channel.getName(), password)
                            .getPayload();

                    channel.setJoin(true);
                    channel.clear();
                    channel.update(pChannel);
                    onSingleClickedChannels(channel);

                    infoAboutMessage(
                            WhatsUp.create(What.JOIN, channel.getName(), csh.getUsername()),
                            false
                    );

                } catch (Exception e) {
                    String failStatus = r(i18n.mapError("join", e));
                    fleetingStatusAsync(failStatus);

                } finally {
                    joinPartSemaphore.release();
                }
            });

        } else {
            // part
            exs.submit(() -> {
                try {
                    csh.cs().part(csh.token(), channel.getName(), "unused");

                    channel.setJoin(false);
                    onSingleClickedChannels(channel);

                    infoAboutMessage(
                            WhatsUp.create(What.PART, channel.getName(), csh.getUsername()),
                            false
                    );

                } catch (Exception e) {
                    String failStatus = r(i18n.mapError("part", e));
                    fleetingStatusAsync(failStatus);

                } finally {
                    joinPartSemaphore.release();
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
                runLater(() -> scroll.setVvalue(1.0));
            }
        });
    }

// ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onKeyMessagePressed(KeyEvent ev) {
        ClientChannel channel = channels.getSelectionModel().getSelectedItem();

        if (ev.getCode() == KeyCode.ENTER) {
            onSendAction(null);
        }

        if (channel != null) {
            channel.setSendCache(message.getText());
        }
    }

    @FXML
    public void onSendAction(ActionEvent ev) {
        if (send.isDisabled()) {
            return;
        }

        String snapshot = message.getText().trim();
        if (snapshot.isEmpty()) {
            return;
        }
        message.setText("");

        ClientChannel channel = channels.getSelectionModel().getSelectedItem();

        if (snapshot.startsWith("/")) {
            exs.submit(() -> onAnyCmd(channel.getName(), snapshot));

        } else {
            exs.submit(() -> {
                try {
                    csh.cs().message(csh.token(), channel.getName(), snapshot);

                } catch (ChatException e) {
                    String failStatus = r(i18n.mapError("message", e));
                    fleetingStatusAsync(failStatus);
                }
            });
        }
    }

// ---------------------------------------------------------------------------------------------------------------

    public void onAnyCmd(String channel, String message) {
        String[] tokens = SPACE_PATTERN.split(message);
        BiConsumer<String, String[]> cmdConsumer = commandsMap.get(tokens[0]);

        if (cmdConsumer != null) {
            cmdConsumer.accept(channel, tokens);

        } else {
            String status = message.equals("/")
                    ? r(i18n.get("cmd.list"))
                    : r(i18n.get("cmd.unknown"));
            fleetingStatusAsync(status);
        }
    }

    public void onSimpleCommand(String channel, String[] tokens, String resMap, SimpleCommand sc) {
        if (tokens.length < 2) {
            String failStatus = r(i18n.get("cmd." + resMap + ".syntax"));
            fleetingStatusAsync(failStatus);
            return;
        }

        try {
            String param = Stream.of(tokens).skip(1).collect(Collectors.joining(" "));
            sc.accept(csh.token(), channel, param);

        } catch (Exception e) {
            String failStatus = r(i18n.mapError(resMap, e));
            fleetingStatusAsync(failStatus);
        }
    }

    public void onStateCommand(String channel, String[] tokens, String resMap, StatefulCommand sc) {
        if (tokens.length < 3
                || !(tokens[2].equals("on") || tokens[2].equals("off"))) {

            String failStatus = r(i18n.get("cmd." + resMap + ".syntax"));
            fleetingStatusAsync(failStatus);
            return;
        }

        try {
            sc.accept(csh.token(), channel, tokens[1], tokens[2].equals("on"));

        } catch (Exception e) {
            String failStatus = r(i18n.mapError(resMap, e));
            fleetingStatusAsync(failStatus);
        }
    }

// ---------------------------------------------------------------------------------------------------------------

    public void onCmdTopic(String channel, String[] tokens) {
        LOG.info("{} {} {}", "onCmdTopic", channel, tokens);

        ChatService cs = csh.cs();
        onSimpleCommand(channel, tokens, "topic", cs::topic);
    }

    public void onCmdKick(String channel, String[] tokens) {
        LOG.info("{} {} {}", "onCmdKick", channel, tokens);

        ChatService cs = csh.cs();
        onSimpleCommand(channel, tokens, "kick", cs::kick);
    }

    public void onCmdBan(String channel, String[] tokens) {
        LOG.info("{} {} {}", "onCmdBan", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "ban", cs::ban);
    }

    public void onCmdAdmin(String channel, String[] tokens) {
        LOG.info("{} {} {}", "onCmdAdmin", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "admin", cs::admin);
    }

    public void onCmdIgnore(String channel, String[] tokens) {
        LOG.info("{} {} {}", "onCmdIgnore", channel, tokens);

        ChatService cs = csh.cs();
        onStateCommand(channel, tokens, "ignore", cs::ignore);
    }

// ---------------------------------------------------------------------------------------------------------------

    private String r(String text) {
        return NL_PATTERN.matcher(text).replaceAll(" ");
    }

    private void fleetingStatusAsync(String text) {
        exs.submit(() -> {
            runLater(() -> status.setText(text));

            exs.schedule(
                    () -> runLater(() -> status.setText("")),
                    5000, TimeUnit.MILLISECONDS
            );
        });
    }
}
