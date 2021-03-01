package com.blax.httpsmugglerapplication;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MainController {

    private final Map<String, String> usernamePasswordRegistry = new HashMap<>();

    private final Map<String, String> sessionRegistry = new ConcurrentHashMap<>();

    MainController() {
        usernamePasswordRegistry.put("bob", "pwd");
        usernamePasswordRegistry.put("alice", "pwd");
    }

    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map> loginJson(@RequestBody UsernamePassword usernamePassword) {
        final String password = usernamePasswordRegistry.get(usernamePassword.getUsername());
        Map<String, String> result = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();

        if (password != null && password.equals(usernamePassword.getPassword())) {
            final UUID sessionId = UUID.randomUUID();

            result.put("success", "true");
            result.put("sessionId", sessionId.toString());

            sessionRegistry.put(sessionId.toString(), usernamePassword.getPassword());
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
        } else {
            result.put("success", "false");
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Map> login(@RequestParam Map map) {
        final String password = usernamePasswordRegistry.get(map.get("username"));
        Map<String, String> result = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();

        if (password != null && password.equals(map.get("password"))) {
            final UUID sessionId = UUID.randomUUID();

            result.put("success", "true");
            result.put("sessionId", sessionId.toString());

            sessionRegistry.put(sessionId.toString(), map.get("username").toString());
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
        } else {
            result.put("success", "false");
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping(value = "/users/me")
    public ResponseEntity<Map> getMe(@RequestHeader("Authorization") String authorization) {

        Map<String, String> result = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();

        final String username = sessionRegistry.get(authorization);
        if (username != null) {
            result.put("success", "true");
            result.put("username", username);
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
        } else {
            result.put("success", "false");
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping(value = "/users/follow")
    public ResponseEntity<Map> follow(@RequestHeader("Authorization") String authorization, @RequestParam("user") String user) {
        Map<String, String> result = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();

        final String username = sessionRegistry.get(authorization);
        if (username != null) {
            result.put("success", "true");
            result.put("follow-user", user);
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
        } else {
            result.put("success", "false");
            return new ResponseEntity<>(result, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }

    @Data
    static class UsernamePassword {
        String username;
        String password;
    }
}
