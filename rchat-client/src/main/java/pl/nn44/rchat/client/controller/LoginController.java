package pl.nn44.rchat.client.controller;

import com.google.common.base.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.controller.ErrorsMapper.EMap;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.protocol.Response;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.runAsync;
import static javafx.application.Platform.runLater;

public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final CsHandler csh;
    private final Consumer<String> sc;
    private final ErrorsMapper em;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Button enter;
    @FXML public Label status;

    public LoginController(CsHandler csHandler,
                           ErrorsMapper errorsMapper,
                           Consumer<String> sceneChanger) {

        this.csh = csHandler;
        this.em = errorsMapper;
        this.sc = sceneChanger;

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runAsync(() -> {
            runLater(() -> {
                status.setText("Initializing app, please wait ...");
                enter.setDisable(true);
            });

            csh.init();

            runLater(() -> {
                status.setText("");
                enter.setDisable(false);
            });

            // csh.test();
        });
    }

    @FXML
    public void onEnterClicked(ActionEvent ev) {
        runAsync(() -> {
            runLater(() -> {
                status.setText("Processing ...");
                username.setDisable(true);
                password.setDisable(true);
                enter.setDisable(true);
            });

            try {
                Response<String> response =
                        csh.getCs().login(
                                username.getText(),
                                Strings.emptyToNull(password.getText())
                        );

                csh.setToken(response.getPayload());

                runLater(() -> {
                    sc.accept("main");
                });

            } catch (Exception e) {
                runLater(() -> {
                    status.setText(em.mapError(EMap.login, e));

                    username.setDisable(false);
                    password.setDisable(false);
                    enter.setDisable(false);
                });
            }
        });
    }
}
