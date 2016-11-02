package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.CsHandler;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final ExecutorService executor;
    private final CsHandler csHandler;
    private final Consumer<String> sceneChanger;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Button enter;
    @FXML public Label status;

    public LoginController(ExecutorService executor, CsHandler csHandler, Consumer<String> sceneChanger) {
        this.executor = executor;
        this.csHandler = csHandler;
        this.sceneChanger = sceneChanger;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    public void initialize() {
        status.setText("Initializing app, please wait ...");
        enter.setDisable(true);

        CompletableFuture
                .runAsync(csHandler::init)
                .thenRunAsync(() -> {
                    status.setText("");
                    enter.setDisable(false);
                });
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
