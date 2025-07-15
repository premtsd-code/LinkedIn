package com.premtsd.linkedin.connectionservice.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Neo4j node representing a person in the connection graph.
 */
@Node("Person")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Unique identifier from the main user system (e.g., MySQL/UserService).
     */
    private Long userId;

    private String name;
}
