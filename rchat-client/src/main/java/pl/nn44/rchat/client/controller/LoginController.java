package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.CsHandler;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final CsHandler csHandler;
    private final Consumer<String> sceneChanger;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Button enter;
    @FXML public Label status;

    public LoginController(CsHandler csHandler, Consumer<String> sceneChanger) {
        this.csHandler = csHandler;
        this.sceneChanger = sceneChanger;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        status.setText("Initializing app, please wait ...");
        enter.setDisable(true);

        CompletableFuture
                .runAsync(csHandler::init)
                .thenRunAsync(() -> {
                    status.setText("");
                    enter.setDisable(false);
                })
                .thenRunAsync(csHandler::runTest);
    }

    int temp = 0;

    @FXML
    public void onEnterClicked(ActionEvent ev) {
        if (temp == 0) {
            status.setText(Integer.toString(new Random().nextInt()));
            temp++;
        } else {
            sceneChanger.accept("main");
        }
    }
}
