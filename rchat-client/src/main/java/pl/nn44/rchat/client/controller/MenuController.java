package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.impl.Clients;
import pl.nn44.rchat.client.impl.CsHandler;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

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
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        protocol.selectToggle(
                protocol.getToggles().get(
                        csHandler.getCurrent()
                )
        );
    }

    @FXML
    public void onExitSelected(ActionEvent ev) {
        stage.close();
    }

    @FXML
    public void onHessianSelected(ActionEvent ev) {
        csHandler.setCurrent(Clients.Cs.Hessian.i());
        LOG.debug("HessianService selected");
    }

    @FXML
    public void onBurlapSelected(ActionEvent ev) {
        csHandler.setCurrent(Clients.Cs.Burlap.i());
        LOG.debug("BurlapService selected");
    }

    @FXML
    public void onXmlRpcSelected(ActionEvent ev) {
        csHandler.setCurrent(Clients.Cs.XmlRpc.i());
        LOG.debug("XmLRpcService selected");
    }

}
