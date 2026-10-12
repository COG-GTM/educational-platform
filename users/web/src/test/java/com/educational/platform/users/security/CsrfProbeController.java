package com.educational.platform.users.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints used to observe CSRF filter decisions:
 * {@code /public} is on the permitted path list, {@code /protected} requires authentication.
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

    @PostMapping("/protected")
    public String protectedProbe() {
        return "ok";
    }

}
