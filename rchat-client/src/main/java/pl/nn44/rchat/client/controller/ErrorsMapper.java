package pl.nn44.rchat.client.controller;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import pl.nn44.rchat.protocol.ChatException;

import java.text.MessageFormat;
import java.util.ResourceBundle;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.ChatException

public class ErrorsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorsMapper.class);

    private ResourceBundle res;

    public ErrorsMapper() {
        this.res = null;
    }

    public String mapError(EMap map, Exception e) {
        if (e instanceof ChatException) {
            ChatException ce = (ChatException) e;
            String resKey = MessageFormat.format("error.{0}.{1}", map.name(), ce.getReason().name());

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

    public void setRes(ResourceBundle res) {
        this.res = res;
    }

    public enum EMap {
        menu,
        login,
        main
    }
}
