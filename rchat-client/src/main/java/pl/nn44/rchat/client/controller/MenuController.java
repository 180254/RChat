package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.SceneChanger;
import pl.nn44.rchat.client.impl.Clients;
import pl.nn44.rchat.client.impl.CsHandler;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class MenuController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MenuController.class);

    private final ScheduledExecutorService exs;
    private final CsHandler csh;
    private final Stage stage;
    private final SceneChanger sc;

    @FXML public MenuItem logout;
    @FXML public MenuItem exit;
    @FXML public MenuItem test;
    @FXML public ToggleGroup protocol;

    public final CopyOnWriteArrayList<Runnable> beforeLogout = new CopyOnWriteArrayList<>();

    // ---------------------------------------------------------------------------------------------------------------

    public MenuController(ScheduledExecutorService executor,
                          CsHandler csHandler,
                          Stage appStage,
                          SceneChanger sceneChanger) {

        this.exs = executor;
        this.csh = csHandler;
        this.stage = appStage;
        this.sc = sceneChanger;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        protocol.selectToggle(
                protocol.getToggles().get(
                        csh.current()
                )
        );
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onLogoutCLicked(ActionEvent ev) {
        beforeLogout.forEach(exs::submit);
        beforeLogout.clear();

        exs.submit(csh::logout);
        sc.accept("login");
    }

    @FXML
    public void onExitClicked(ActionEvent ev) {
        stage.close();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onProtocolChanged(ActionEvent ev) {
        exs.submit(() -> {
            Toggle source = (Toggle) ev.getSource();
            int protocolIndex = protocol.getToggles().indexOf(source);
            Clients.Cs cs = Clients.Cs.byIndex(protocolIndex);

            csh.setCurrent(cs.i());
            LOG.debug("{}Client selected", cs.name());
        });
    }

    @FXML
    public void onTextClicked(ActionEvent ev) {
        exs.submit(csh::test);
    }
}
