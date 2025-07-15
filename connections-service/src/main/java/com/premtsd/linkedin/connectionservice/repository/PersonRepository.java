package com.premtsd.linkedin.connectionservice.repository;

import com.premtsd.linkedin.connectionservice.entity.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for performing graph operations on Person nodes in Neo4j.
 */
public interface PersonRepository extends Neo4jRepository<Person, Long> {

    /**
     * Find a person by their name.
     */
    Optional<Person> getByName(String name);

    /**
     * Retrieve all first-degree (direct) connections for a given user.
     */
    @Query("""
           MATCH (personA:Person)-[:CONNECTED_TO]-(personB:Person)
           WHERE personA.userId = $userId
           RETURN personB
           """)
    List<Person> getFirstDegreeConnections(Long userId);

    /**
     * Check if a connection request already exists from sender to receiver.
     */
    @Query("""
           MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person)
           WHERE p1.userId = $senderId AND p2.userId = $receiverId
           RETURN count(r) > 0
           """)
    boolean connectionRequestExists(Long senderId, Long receiverId);

    /**
     * Check if two users are already connected.
     */
    @Query("""
           MATCH (p1:Person)-[r:CONNECTED_TO]-(p2:Person)
           WHERE p1.userId = $senderId AND p2.userId = $receiverId
           RETURN count(r) > 0
           """)
    boolean alreadyConnected(Long senderId, Long receiverId);

    /**
     * Create a REQUESTED_TO relationship from sender to receiver.
     */
    @Query("""
           MATCH (p1:Person), (p2:Person)
           WHERE p1.userId = $senderId AND p2.userId = $receiverId
           CREATE (p1)-[:REQUESTED_TO]->(p2)
           """)
    void addConnectionRequest(Long senderId, Long receiverId);

    /**
     * Accept a connection request: delete REQUESTED_TO and create CONNECTED_TO.
     */
    @Query("""
           MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person)
           WHERE p1.userId = $senderId AND p2.userId = $receiverId
           DELETE r
           CREATE (p1)-[:CONNECTED_TO]->(p2)
           """)
    void acceptConnectionRequest(Long senderId, Long receiverId);

    /**
     * Reject a connection request: delete REQUESTED_TO relationship.
     */
    @Query("""
           MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person)
           WHERE p1.userId = $senderId AND p2.userId = $receiverId
           DELETE r
           """)
    void rejectConnectionRequest(Long senderId, Long receiverId);
}
