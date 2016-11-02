package pl.nn44.rchat.client;

import com.google.common.base.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.controller.LoginController;
import pl.nn44.rchat.client.controller.MainController;
import pl.nn44.rchat.client.controller.MenuController;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.ChatException

public class ClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ClientApp.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CsHandler csHandler = new CsHandler();
    private final Map<Class<?>, Supplier<Object>> controllers = new HashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        Consumer<String> sceneChanger = (scene) -> {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                URL fxmlResource = classLoader.getResource("layout/" + scene + ".fxml");
                if (fxmlResource == null) {
                    throw new IOException("no such scene: " + scene);
                }

                FXMLLoader loader = new FXMLLoader();
                loader.setControllerFactory(clazz -> controllers.get(clazz).get());
                loader.setLocation(fxmlResource);

                Parent fxmlParent = loader.load();
                stage.setScene(new Scene(fxmlParent));

            } catch (IOException e) {
                throw new AssertionError(e);
            }
        };

        controllers.put(LoginController.class, () -> new LoginController(executor, csHandler, sceneChanger));
        controllers.put(MainController.class, () -> new MainController(executor, csHandler));
        controllers.put(MenuController.class, () -> new MenuController(executor, csHandler, stage));

        sceneChanger.accept("login");
        stage.setTitle("RChat");
        stage.show();
    }

    @Override
    public void stop() {
        executor.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        LOG.debug("App started.");
        Application.launch(ClientApp.class, args);
    }
}
