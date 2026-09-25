package com.frietsync.backend.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckController {

    @GetMapping("/api/check")
    public Map<String, String> check() {
        return Map.of("status", "ok");
    }
}
