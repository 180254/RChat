package pl.nn44.rchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Toggle;
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
    public void onExitClicked(ActionEvent ev) {
        stage.close();
    }

    @FXML
    public void onProtocolChanged(ActionEvent actionEvent) {
        Toggle source = (Toggle) actionEvent.getSource();
        int protocolIndex = protocol.getToggles().indexOf(source);
        Clients.Cs cs = Clients.Cs.byIndex(protocolIndex);

        csHandler.setCurrent(cs.i());
        LOG.debug("{}Client selected", cs.name());
    }
}
