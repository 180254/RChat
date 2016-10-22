package pl.nn44.rchat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PropLoader.class);

    public static Properties get() {
        Properties prop = new Properties();

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream("app.properties");
            prop.load(stream);

        } catch (IOException e) {
            LOG.error("Unable to load .properties file.", e);
        }

        return prop;
    }
}
