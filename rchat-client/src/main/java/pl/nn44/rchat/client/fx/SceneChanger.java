package pl.nn44.rchat.client.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SceneChanger implements Consumer<String> {

    private static final Logger LOG = LoggerFactory.getLogger(SceneChanger.class);

    private final Stage primaryStage;
    private final Callback<Class<?>, Object> controllerFactory;
    private final ResourceBundle resources;

    public SceneChanger(Stage primaryStage,
                        Callback<Class<?>, Object> controllerFactory,
                        ResourceBundle resources) {

        this.primaryStage = primaryStage;
        this.controllerFactory = controllerFactory;
        this.resources = resources;
    }

    @Override
    public void accept(String scene) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL fxmlResource = classLoader.getResource("layout/" + scene + ".fxml");
            if (fxmlResource == null) {
                throw new IOException("no such scene: " + scene);
            }

            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(controllerFactory);
            loader.setLocation(fxmlResource);
            loader.setResources(resources);

            Parent fxmlParent = loader.load();
            primaryStage.setScene(new Scene(fxmlParent));

        } catch (IOException e) {
            LOG.error("Unable to change scene.", e);
            throw new AssertionError(e);
        }
    }
}
