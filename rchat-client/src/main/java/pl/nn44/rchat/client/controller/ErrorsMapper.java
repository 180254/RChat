package pl.nn44.rchat.client.controller;

import com.google.common.collect.ImmutableMap;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatException.Reason;

import java.util.Map;

// org.springframework.remoting.RemoteAccessException
// org.apache.xmlrpc.XmlRpcException
// pl.nn44.rchat.protocol.ChatException

public class ErrorsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorsMapper.class);

    public static final String CONNECTION_ERROR = "Server is currently unreachable. Please try again later.";
    public static final String SERVER_IMPL_FAILURE = "Something went wrong on server end. Internal code: 0x0010FA0Y.";
    public static final String DEFAULT_MAP_REASON = "Something went wrong on server end. Internal code: 0x0010FA0Z.";

    public final Map<Reason, String> login = ImmutableMap.<Reason, String>builder()
            .put(Reason.ALREADY_LOGGED_IN, "You are already logged in.")
            .put(Reason.GIVEN_BAD_PASSWORD, "Given credentials are incorrect.")
            .put(Reason.GIVEN_BAD_USERNAME,
                    "Given username is forbidden.\n" +
                            "Allowed length: from 1 up to 10.\n" +
                            "Allowed chars:: a-zA-Z0-9-_.\n")
            .build();

    public String mapError(Map<?, String> map, Exception e) {
        return e instanceof ChatException
                ? mapError(map, (ChatException) e)
                : mapError(e);
    }

    public String mapError(Map<?, String> map, ChatException e) {
        return map.getOrDefault(e.getReason(), DEFAULT_MAP_REASON);
    }

    public String mapError(Exception e) {
        LOG.error("mapped exception", e);

        return e instanceof RemoteAccessException || e instanceof XmlRpcException
                ? CONNECTION_ERROR
                : SERVER_IMPL_FAILURE;
    }

}
