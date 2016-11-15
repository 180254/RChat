package pl.nn44.rchat.client.util;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import pl.nn44.rchat.protocol.exception.ChatException;

import java.text.MessageFormat;
import java.util.ResourceBundle;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.exception.ChatException

public class LocaleHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleHelper.class);

    private ResourceBundle res;

    public LocaleHelper() {
        this.res = null;
    }

    public String mapError(String map, Exception e) {
        if (e instanceof ChatException) {
            ChatException ce = (ChatException) e;
            String resKey = MessageFormat.format("error.{0}.{1}", map, ce.getReason().name());

            return res.containsKey(resKey)
                    ? res.getString(resKey)
                    : res.getString("any.default-err-reason");

        } else {
            LOG.error("Exception", e);

            if (e instanceof RemoteAccessException || e instanceof XmlRpcException) {
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

    public void setRes(ResourceBundle res) {
        this.res = res;
    }
}
