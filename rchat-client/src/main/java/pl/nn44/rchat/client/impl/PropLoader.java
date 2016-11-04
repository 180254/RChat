package pl.nn44.rchat.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PropLoader.class);

    public static Properties get() {
        try {
            Properties prop = new Properties();

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream("prop/app.properties");
            prop.load(stream);

            LOG.debug("Loaded properties. {}", prop);
            return prop;

        } catch (IOException e) {
            LOG.error("Unable to load .properties file.", e);
            throw new AssertionError(e);
        }
    }
}
