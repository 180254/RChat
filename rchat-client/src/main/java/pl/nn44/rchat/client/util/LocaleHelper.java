package pl.nn44.rchat.client.util;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import pl.nn44.rchat.protocol.ChatException;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.ChatException

public class LocaleHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleHelper.class);
    private static final Pattern NL_PATTERN = Pattern.compile("\n");

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

    public String mapError2(String map, Exception e) {
        return NL_PATTERN.matcher(mapError(map, e)).replaceAll(" ");
    }

    public String get(String key) {
        return res.getString(key);
    }

    public String get2(String key) {
        return NL_PATTERN.matcher(get(key)).replaceAll(" ");
    }

    public void setRes(ResourceBundle res) {
        this.res = res;
    }
}
