package pl.nn44.rchat.server;

import org.springframework.boot.autoconfigure.web.AbstractErrorController;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

@RequestMapping("/error")
public class PlainErrorController extends AbstractErrorController {

    public PlainErrorController() {
        super(new DefaultErrorAttributes(), Collections.emptyList());
    }

    @Override
    public String getErrorPath() {
        return "";
    }

    @RequestMapping(produces = {"text/plain"})
    public ResponseEntity<String> plainError(
            HttpServletRequest request
    ) {
        HttpStatus status = getStatus(request);
        Map<String, Object> errorAttributes = getErrorAttributes(request, false);

        String message = MessageFormat.format("{0} {1}",
                errorAttributes.get("status"),
                errorAttributes.get("error"));

        return new ResponseEntity<>(message, status);
    }
}
