package com.moamu.domain.artwork.client;

import lombok.Getter;

@Getter
public class ArtworkGenerationClientException extends RuntimeException {
    public enum Reason {
        TASK_NOT_FOUND, PAYLOAD_CONFLICT, HTTP_ERROR, CONNECTION_ERROR, INVALID_RESPONSE
    }

    private final Reason reason;
    private final Integer httpStatus;

    public ArtworkGenerationClientException(Reason reason, Integer httpStatus, Throwable cause) {
        super("Artwork generation request failed: " + reason, cause);
        this.reason = reason;
        this.httpStatus = httpStatus;
    }
}
