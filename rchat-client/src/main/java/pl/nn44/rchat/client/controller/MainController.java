package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.impl.CsHandler;

import java.util.Random;

public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private final CsHandler csHandler;

    @FXML public Label status;
    @FXML public Button send;

    public MainController(CsHandler csHandler) {
        this.csHandler = csHandler;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    public void sendClicked(ActionEvent ev) {
        status.setText(Integer.toString(new Random().nextInt()));
    }
}
