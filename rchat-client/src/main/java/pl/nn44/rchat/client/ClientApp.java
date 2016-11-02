package pl.nn44.rchat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.controller.LoginController;
import pl.nn44.rchat.client.controller.MainController;
import pl.nn44.rchat.client.controller.MenuController;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.ChatException

public class ClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ClientApp.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CsHandler csHandler = new CsHandler(executor);
    private final Map<Class<?>, Object> controllers = new HashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        csHandler.init();
        csHandler.runTestAsync();
        controllers.put(LoginController.class, new LoginController(csHandler));
        controllers.put(MainController.class, new MainController(csHandler));
        controllers.put(MenuController.class, new MenuController(csHandler, stage));

        URL fxmlLayout = getClass().getClassLoader().getResource("layout/main.fxml");
        if (fxmlLayout == null) {
            throw new AssertionError();
        }

        FXMLLoader loader = new FXMLLoader(fxmlLayout);
        loader.setControllerFactory(controllers::get);

        VBox root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("RChat");
        stage.show();
    }

    @Override
    public void stop() {
        executor.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        LOG.info("App started.");
        Application.launch(ClientApp.class, args);
    }

}
