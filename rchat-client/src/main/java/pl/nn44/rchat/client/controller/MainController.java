package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.CsHandler;

import java.security.SecureRandom;
import java.util.Random;

public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Random random = new SecureRandom();
    private final CsHandler csHandler;

    @FXML
    private ToggleGroup protocol;

    @FXML
    private Label status;

    @FXML
    private Button send;

    public MainController(CsHandler csHandler) {
        this.csHandler = csHandler;
        LOG.info("MainController instance created.");
    }

    public void sendClicked(ActionEvent actionEvent) {
        status.setText(Integer.toString(random.nextInt()));
        csHandler.getCs(); // just suppress warning
    }
}
