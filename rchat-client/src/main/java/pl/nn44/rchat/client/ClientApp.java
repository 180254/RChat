package pl.nn44.rchat.client;

import com.google.common.base.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.controller.LoginController;
import pl.nn44.rchat.client.controller.MainController;
import pl.nn44.rchat.client.controller.MenuController;
import pl.nn44.rchat.client.fx.SceneChanger;
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.CompletableFuture.runAsync;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.exception.ChatException

public class ClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ClientApp.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final CsHandler csHandler = new CsHandler();
    private final LocaleHelper locHelper = new LocaleHelper();
    private final Map<Class<?>, Supplier<Object>> controllers = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.getIcons().add(new Image("layout/icon.png"));

        ResourceBundle resources = PropertyResourceBundle.getBundle("strings/strings");
        locHelper.setResources(resources);

        SceneChanger sceneChanger = new SceneChanger(
                primaryStage,
                clazz -> controllers.get(clazz).get(),
                resources
        );

        controllers.put(LoginController.class, () ->
                new LoginController(executor, csHandler, locHelper, sceneChanger)
        );
        controllers.put(MainController.class, () ->
                new MainController(executor, csHandler, locHelper))
        ;
        controllers.put(MenuController.class, () ->
                new MenuController(executor, csHandler, primaryStage, sceneChanger)
        );

        sceneChanger.accept("login");
        primaryStage.setTitle("RChat");
        primaryStage.show();
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        Platform.exit();

        runAsync(csHandler::logout)
                .thenRun(() -> System.exit(0));
    }

    public static void main(String[] args) {
        LOG.debug("App started.");
        Application.launch(ClientApp.class, args);
    }
}
