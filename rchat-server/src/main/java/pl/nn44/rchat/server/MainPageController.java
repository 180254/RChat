package pl.nn44.rchat.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
public class MainPageController {

    @RequestMapping(value = "/", produces = {"text/plain"})
    public ResponseEntity<String> plainMain() {

        return new ResponseEntity<>("", HttpStatus.OK);
    }
}

