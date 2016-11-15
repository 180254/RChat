package pl.nn44.rchat.server.page;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
public class PlainPageController {

    @RequestMapping(value = "/", produces = {"text/plain"})
    public ResponseEntity<String> plainMain() {

        return new ResponseEntity<>("", HttpStatus.OK);
    }
}

