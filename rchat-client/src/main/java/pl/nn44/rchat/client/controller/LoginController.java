package pl.nn44.rchat.client.controller;

import com.google.common.base.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.fx.SceneChanger;
import pl.nn44.rchat.client.fx.TitleChanger;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

import static javafx.application.Platform.runLater;

public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final ScheduledExecutorService exs;
    private final CsHandler csh;
    private final LocaleHelper i18n;
    private final SceneChanger sc;
    private final TitleChanger tc;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Button enter;
    @FXML public Label status;

    @FXML public MenuBar menu;
    @FXML public MenuController menuController;

    // ---------------------------------------------------------------------------------------------------------------

    public LoginController(ScheduledExecutorService executor,
                           CsHandler csHandler,
                           LocaleHelper locHelper,
                           SceneChanger sceneChanger,
                           TitleChanger titleChanger) {

        this.exs = executor;
        this.csh = csHandler;
        this.i18n = locHelper;
        this.sc = sceneChanger;
        this.tc = titleChanger;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        exs.submit(() -> {

            String initializingStatus = i18n.get("ctrl.login.initializing");
            runLater(() -> {
                tc.accept(null);
                menuController.logout.setDisable(true);
                status.setText(initializingStatus);
                enter.setDisable(true);
            });

            csh.init();

            runLater(() -> {
                status.setText("");
                enter.setDisable(false);
            });
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    @FXML
    public void onEnterClicked(ActionEvent ev) {
        exs.submit(() -> {

            String processingStatus = i18n.get("ctrl.login.processing");
            runLater(() -> {
                status.setText(processingStatus);
                username.setDisable(true);
                password.setDisable(true);
                enter.setDisable(true);
            });

            try {
                String token =
                        csh.cs().login(
                                username.getText(),
                                Strings.emptyToNull(password.getText())
                        ).getPayload();

                csh.setUsername(username.getText());
                csh.setToken(token);

                runLater(() -> {
                    sc.accept("main");
                    tc.accept(csh.getUsername());
                });

            } catch (Exception e) {
                String errorStatus = i18n.mapError("login", e);
                runLater(() -> {
                    status.setText(errorStatus);
                    username.setDisable(false);
                    password.setDisable(false);
                    enter.setDisable(false);
                });
            }
        });
    }
}
