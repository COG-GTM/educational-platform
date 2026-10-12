package com.educational.platform.users.security;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint on the permitted {@code /public} path, used to observe CSRF filter decisions.
 */
@RestController
public class CsrfProbeController {

    @PostMapping("/public")
    public String probe() {
        return "ok";
    }

}
