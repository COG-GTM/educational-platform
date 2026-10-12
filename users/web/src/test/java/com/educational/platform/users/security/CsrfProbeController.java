package com.educational.platform.users.security;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints used to observe CSRF filter decisions:
 * {@code /public} is on the permitted path list, {@code /protected} requires authentication and
 * {@code /h2-console/probe} sits under the CSRF-exempt H2 console path.
 */
@RestController
public class CsrfProbeController {

    @PostMapping("/public")
    public String probe() {
        return "ok";
    }

    @GetMapping("/public")
    public String read() {
        return "ok";
    }

    @PutMapping("/public")
    public String update() {
        return "ok";
    }

    @DeleteMapping("/public")
    public String remove() {
        return "ok";
    }

    @PostMapping("/protected")
    public String protectedProbe() {
        return "ok";
    }

    @PostMapping("/h2-console/probe")
    public String h2ConsoleProbe() {
        return "ok";
    }

}
