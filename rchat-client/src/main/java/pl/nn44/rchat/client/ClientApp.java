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
import pl.nn44.rchat.client.impl.CsHandler;
import pl.nn44.rchat.client.util.LocaleHelper;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.runAsync;

public class ClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ClientApp.class);

    private final CsHandler csHandler = new CsHandler();
    private final LocaleHelper locHelper = new LocaleHelper();
    private final Map<Class<?>, Supplier<Object>> controllers = new HashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        ResourceBundle res = PropertyResourceBundle.getBundle("prop/strings");
        locHelper.setRes(res);

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
                loader.setResources(res);

                Parent fxmlParent = loader.load();
                stage.setScene(new Scene(fxmlParent));

            } catch (IOException e) {
                LOG.error("Unable to change scene.", e);
                throw new AssertionError(e);
            }
        };

        controllers.put(LoginController.class, () ->
                new LoginController(csHandler, locHelper, sceneChanger)
        );
        controllers.put(MainController.class, () ->
                new MainController(csHandler))
        ;
        controllers.put(MenuController.class, () ->
                new MenuController(csHandler, stage, sceneChanger)
        );

        sceneChanger.accept("login");
        stage.setTitle("RChat");
        stage.show();
    }

    @Override
    public void stop() {
        runAsync(csHandler::logout);
        Platform.exit();
    }

    public static void main(String[] args) {
        LOG.debug("App started.");
        Application.launch(ClientApp.class, args);
    }
}
