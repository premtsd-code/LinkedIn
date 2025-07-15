package com.premtsd.linkedin.connectionservice.event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event representing that a connection request has been accepted.
 * Typically sent via Kafka or internal event bus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptConnectionRequestEvent implements Serializable {

    @NotNull
    private Long senderId;
    @NotNull
    private Long receiverId;
}
