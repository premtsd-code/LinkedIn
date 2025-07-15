package com.premtsd.linkedin.connectionservice.controller;

import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing user connections (like LinkedIn).
 */
@Slf4j
@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class ConnectionsController {

    private final ConnectionsService connectionsService;

    /**
     * Returns the list of first-degree (direct) connections for the logged-in user.
     */
    @GetMapping("/first-degree")
    public ResponseEntity<List<Person>> getFirstConnections() {
        log.info("Fetching first-degree connections for current user");
        return ResponseEntity.ok(connectionsService.getFirstDegreeConnections());
    }

    /**
     * Sends a connection request from the current user to the specified user.
     */
    @PostMapping("/request/{userId}")
    public ResponseEntity<Boolean> sendConnectionRequest(@PathVariable Long userId) {
        log.info("Sending connection request to userId: {}", userId);
        return ResponseEntity.ok(connectionsService.sendConnectionRequest(userId));
    }

    /**
     * Accepts a connection request from the specified user.
     */
    @PostMapping("/accept/{userId}")
    public ResponseEntity<Boolean> acceptConnectionRequest(@PathVariable Long userId) {
        log.info("Accepting connection request from userId: {}", userId);
        return ResponseEntity.ok(connectionsService.acceptConnectionRequest(userId));
    }

    /**
     * Rejects a connection request from the specified user.
     */
    @PostMapping("/reject/{userId}")
    public ResponseEntity<Boolean> rejectConnectionRequest(@PathVariable Long userId) {
        log.info("Rejecting connection request from userId: {}", userId);
        return ResponseEntity.ok(connectionsService.rejectConnectionRequest(userId));
    }
}
