package pl.nn44.rchat.client.controller;

import com.google.common.base.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.util.LocaleHelper;
import pl.nn44.rchat.protocol.Response;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static javafx.application.Platform.runLater;

public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final ExecutorService exs;
    private final CsHandler csh;
    private final LocaleHelper i18n;
    private final Consumer<String> sc;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Button enter;
    @FXML public Label status;

    @FXML public MenuBar menu;
    @FXML public MenuController menuController;

    // ---------------------------------------------------------------------------------------------------------------

    public LoginController(ExecutorService executor,
                           CsHandler csHandler,
                           LocaleHelper locHelper,
                           Consumer<String> sceneChanger) {

        this.csh = csHandler;
        this.exs = executor;
        this.i18n = locHelper;
        this.sc = sceneChanger;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        exs.submit(() -> {
            runLater(() -> {
                menuController.logout.setDisable(true);
                status.setText(i18n.get("ctrl.login.initializing"));
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
            runLater(() -> {
                status.setText(i18n.get("ctrl.login.processing"));
                username.setDisable(true);
                password.setDisable(true);
                enter.setDisable(true);
            });

            try {
                Response<String> response =
                        csh.cs().login(
                                username.getText(),
                                Strings.emptyToNull(password.getText())
                        );

                csh.setToken(response.getPayload());

                runLater(() -> {
                    sc.accept("main");
                });

            } catch (Exception e) {
                runLater(() -> {
                    status.setText(i18n.mapError("login", e));

                    username.setDisable(false);
                    password.setDisable(false);
                    enter.setDisable(false);
                });
            }
        });
    }
}
