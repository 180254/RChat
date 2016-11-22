package pl.nn44.rchat.client.util;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import pl.nn44.rchat.protocol.exception.ChatException;

import java.lang.reflect.UndeclaredThrowableException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class LocaleHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleHelper.class);

    private ResourceBundle res;

    // ---------------------------------------------------------------------------------------------------------------

    public LocaleHelper() {
        this.res = null;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void setResources(ResourceBundle resources) {
        this.res = resources;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String mapError(String map, Exception e) {
        Throwable t = e;

        // unpack UndeclaredThrowableException caused by using proxy
        if (t instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException ue = (UndeclaredThrowableException) t;
            if (ue.getUndeclaredThrowable() != null) {
                t = ue.getUndeclaredThrowable();
            }
        }

        if (t instanceof ChatException) {
            LOG.warn("Exception [{}] {}", map.toUpperCase(), t.toString());

            ChatException ce = (ChatException) t;
            String resKey = MessageFormat.format("error.{0}.{1}", map, ce.getReason().name());

            return res.containsKey(resKey)
                    ? res.getString(resKey)
                    : res.getString("any.default-err-reason");

        } else {
            LOG.error("Exception [{}] {}", map.toUpperCase(), t);

            if (t instanceof RemoteAccessException || t instanceof XmlRpcException) {
                return res.getString("any.remote-conn-error");
            } else {
                return res.getString("any.server-impl-failure");
            }
        }
    }

    public String get(String key, Object... values) {
        String resText = res.getString(key);
        return values.length > 0
                ? MessageFormat.format(resText, values)
                : resText;
    }
}
