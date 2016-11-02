package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.CsHandler;

import java.util.Random;

public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private final CsHandler csHandler;

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Label status;

    public LoginController(CsHandler csHandler) {
        this.csHandler = csHandler;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    public void onEnterClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
    }
}
