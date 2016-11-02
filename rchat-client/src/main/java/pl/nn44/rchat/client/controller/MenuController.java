package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.CsHandler;

public class MenuController {

    private static final Logger LOG = LoggerFactory.getLogger(MenuController.class);
    private final CsHandler csHandler;
    private final Stage stage;

    @FXML public ToggleGroup protocol;

    public MenuController(CsHandler csHandler, Stage stage) {
        this.csHandler = csHandler;
        this.stage = stage;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    @FXML
    public void onExitSelected(ActionEvent ev) {
        stage.close();
    }

    @FXML
    public void onHessianSelected(ActionEvent ev) {
        LOG.info("onHessianSelected");
    }

    @FXML
    public void onBurlapSelected(ActionEvent ev) {
        LOG.info("onBurlapSelected");
    }

    @FXML
    public void onXmlRpcSelected(ActionEvent ev) {
        LOG.info("onXmlRpcSelected");
    }
}
