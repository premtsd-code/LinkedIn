package com.premtsd.linkedin.connectionservice.controller;

import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.repository.PersonRepository;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class ConnectionsController {

//    @Autowired
//    private final PersonRepository personRepository;
//
//    @PostConstruct
//    public void initData() {
//        Person p1 = new Person();
//        p1.setUserId(1L);
//        p1.setName("Alice");
//
//        Person p2 = new Person();
//        p2.setUserId(2L);
//        p2.setName("Bob");
//
//        personRepository.save(p1);
//        personRepository.save(p2);
//    }


    private final ConnectionsService connectionsService;

    @GetMapping("/first-degree")
    public ResponseEntity<List<Person>> getFirstConnections() {
        return ResponseEntity.ok(connectionsService.getFirstDegreeConnections());
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Boolean> sendConnectionRequest(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.sendConnectionRequest(userId));
    }

    @PostMapping("/accept/{userId}")
    public ResponseEntity<Boolean> acceptConnectionRequest(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.acceptConnectionRequest(userId));
    }

    @PostMapping("/reject/{userId}")
    public ResponseEntity<Boolean> rejectConnectionRequest(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.rejectConnectionRequest(userId));
    }
}
