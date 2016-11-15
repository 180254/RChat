package pl.nn44.rchat.client.impl;

import com.google.common.base.MoreObjects;
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
            InputStream stream = loader.getResourceAsStream("app.properties");
            prop.load(stream);

            // may be overridden by system properties
            prop.forEach((key, value) -> {
                String _key = (String) key;
                String _value = (String) value;

                prop.setProperty(_key,
                        MoreObjects.firstNonNull(
                                System.getProperty(_key),
                                _value
                        )
                );
            });

            LOG.debug("Loaded properties. {}", prop);
            return prop;

        } catch (IOException e) {
            LOG.error("Unable to load .properties file.", e);
            throw new AssertionError(e);
        }
    }
}
