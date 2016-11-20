package pl.nn44.rchat.client.controller;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
    private static final int WHATS_UP_LONG_POOLING = (int) TimeUnit.MINUTES.toMillis(1);

    private final ScheduledExecutorService exs;
    private final CsHandler csh;
    private final LocaleHelper i18n;

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

    public RefreshableListViewSkin<ClientChannel> channelsSkin;
    public RefreshableListViewSkin<ClientUser> usersSkin;

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

    private final ConcurrentHashMap<String, ClientChannel> channelsMap =
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

    public MainController(ScheduledExecutorService executor,
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

        topicModel = topic.textProperty();
        usersModel = FXCollections.synchronizedObservableList(users.getItems());
        messagesModel = FXCollections.synchronizedObservableList(messages.getChildren());

        menuController.beforeLogout.add(() -> newsFuture.cancel(true));

        exs.submit(() -> {
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
                    status.setText("");
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

    public void infoAboutSimpleMsg(WhatsUp whatsUp) {
        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];

        String locMap = whatsUp.getWhat().name().toLowerCase();
        PrintInfo ctMsgInfo = new PrintInfo(
                i18n, time, "whats-up." + locMap, whatsUp.getParams()
        );

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    public void infoAboutStatefulMsg(WhatsUp whatsUp) {
        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        boolean state = whatsUp.getParams()[3].equals("ON");
        String code = state ? "1" : "0";

        String locMap = whatsUp.getWhat().name().toLowerCase();
        PrintInfo ctMsgInfo = new PrintInfo(
                i18n, time, "whats-up." + locMap + "." + code, whatsUp.getParams()
        );

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getMessages().addAll(ctMsgInfo.toNodes());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeNothing(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeNothing", whatsUp);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeJoin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeJoin", whatsUp);

        infoAboutSimpleMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoJoined = whatsUp.getParams()[1];
        boolean auth = Boolean.parseBoolean(whatsUp.getParams()[2]);
        boolean admin = Boolean.parseBoolean(whatsUp.getParams()[3]);

        User user = new User(channel, whoJoined, auth, false, admin, false);
        ClientUser clientUser = new ClientUser(user);

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().add(clientUser);
    }

    public void onSomePart(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePart", whatsUp);

        infoAboutSimpleMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoPart = whatsUp.getParams()[1];

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(whoPart));
    }

    public void onSomeTopic(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeTopic", whatsUp);

        infoAboutSimpleMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String someText = whatsUp.getParams()[2];

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.setTopic(someText);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeKick(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeKick", whatsUp);

        infoAboutSimpleMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoKicked = whatsUp.getParams()[1];

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().removeIf(u -> u.getUsername().equals(whoKicked));

        // special case: info about my part
        // kicked by admin - server removed mi from channel
        if (whoKicked.equals(csh.getUsername())) {
            onDoubleClickedChannels(ctChannel);
        }
    }

    public void onSomeBan(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeBan", whatsUp);

        infoAboutStatefulMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoBanned = whatsUp.getParams()[1];
        boolean state = whatsUp.getParams()[3].equals("ON");

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(whoBanned))
                .forEach(u -> u.setBanned(state));

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeAdmin(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeAdmin", whatsUp);

        infoAboutStatefulMsg(whatsUp);

        String channel = whatsUp.getParams()[0];
        String whoAdmin = whatsUp.getParams()[1];
        boolean state = whatsUp.getParams()[3].equals("ON");

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getUsers().stream()
                .filter(u -> u.getUsername().equals(whoAdmin))
                .forEach(u -> u.setAdmin(state));

        runLater(() -> usersSkin.refresh());
    }

    public void onSomeIgnore(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeIgnore", whatsUp);

        ClientChannel channel = channels.getSelectionModel().getSelectedItem();
        if (channel != null) {
            // replace param-channel with current channel
            // in protocol, for this WhatsUp, channel pos is unused
            String[] newParams = whatsUp.getParams();
            newParams[0] = channel.getName();

            WhatsUp whatsUp2 = WhatsUp.create(whatsUp.getWhat(), newParams);
            infoAboutStatefulMsg(whatsUp2);
        }

        String whoIgnored = whatsUp.getParams()[1];
        boolean state = whatsUp.getParams()[3].equals("ON");

        // if i ignored (not i am ignored) set ignored flag
        if (!whoIgnored.equals(csh.getUsername())) {
            channelsMap.values().stream()
                    .flatMap(c -> c.getUsers().stream())
                    .filter(u -> u.getUsername().equals(whoIgnored))
                    .forEach(u -> u.setIgnored(state));

            runLater(() -> usersSkin.refresh());
        }
    }
    // ---------------------------------------------------------------------------------------------------------------

    public void onSomeMessage(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomeMessage", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String channel = whatsUp.getParams()[0];
        String whoMsg = whatsUp.getParams()[1];
        String someText = whatsUp.getParams()[2];

        PrintMsg printMsg = new PrintMsg(whoMsg, time, someText);

        ClientChannel ctChannel = channelsMap.get(channel);
        ctChannel.getMessages().addAll(printMsg.toNodes());
    }

    public void onSomePrivy(WhatsUp whatsUp) {
        LOG.info("{} {}", "onSomePrivy", whatsUp);

        LocalDateTime time = whatsUp.getTime();
        String whoMsgTo = whatsUp.getParams()[0];
        String whoMsgBy = whatsUp.getParams()[0];
        String someText = whatsUp.getParams()[2];

        LOG.info("?", time, whoMsgTo, whoMsgBy, someText);
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
        boolean sendDisable = fatalFail || !channel.isJoin();
        String sendCurrentMsg = channel.getCurrentMsg();
        runLater(() -> {
            send.setDisable(sendDisable);
            message.setText(sendCurrentMsg);
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
            exs.submit(() -> {
                try {
                    Channel pChannel = csh.cs()
                            .join(csh.token(), channel.getName(), null)
                            .getPayload();

                    channel.setJoin(true);
                    channel.clear();
                    channel.update(pChannel);
                    onSingleClickedChannels(channel);

                    infoAboutSimpleMsg(
                            WhatsUp.create(What.JOIN, channel.getName(), csh.getUsername())
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

                    infoAboutSimpleMsg(
                            WhatsUp.create(What.PART, channel.getName(), csh.getUsername())
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

        ClientChannel channel = channels.getSelectionModel().getSelectedItem();

        if (message.startsWith("/")) {
            exs.submit(() -> onAnyCmd(channel.getName(), message));

        } else {
            exs.submit(() -> {
                try {
                    csh.cs().message(csh.token(), channel.getName(), message);

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

    public void onSimpleCommand(String channel, String[] tokens, String locMap, SimpleCommand sc) {
        if (tokens.length < 2) {
            String failStatus = r(i18n.get("cmd." + locMap + ".syntax"));
            fleetingStatusAsync(failStatus);
            return;
        }

        try {
            String param = Stream.of(tokens).skip(1).collect(Collectors.joining(" "));
            sc.accept(csh.token(), channel, param);

        } catch (Exception e) {
            String failStatus = r(i18n.mapError(locMap, e));
            fleetingStatusAsync(failStatus);
        }
    }

    public void onStateCommand(String channel, String[] tokens, String locMap, StatefulCommand sc) {
        String failStatus = r(i18n.get("cmd." + locMap + ".syntax"));

        if (tokens.length < 3) {
            fleetingStatusAsync(failStatus);
            return;
        }

        if (!(tokens[2].equals("on") || tokens[2].equals("off"))) {
            fleetingStatusAsync(failStatus);
            return;
        }

        try {
            sc.accept(csh.token(), channel, tokens[1], tokens[2].equals("on"));

        } catch (Exception e) {
            failStatus = r(i18n.mapError(locMap, e));
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
                    3500, TimeUnit.MILLISECONDS
            );
        });
    }
}
