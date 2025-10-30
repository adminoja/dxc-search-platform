package th.go.dxc.platform.search.domain.search.model;

public enum LocalSearchStatus {
    // Normal outcomes
    PENDING,
    IN_PROGRESS,
    SUCCESS,      // >= 1 item returned
    EMPTY,        // 0 items returned (not an error)
    PARTIAL,      // some data but known truncation/partial failure

    // Client-side problems
    INVALID_REQUEST, // bad params, unsupported field, etc.
    UNAUTHORIZED,    // 401
    FORBIDDEN,       // 403
    NOT_FOUND,       // 404 (dataset endpoint missing)

    // Throttling / time
    RATE_LIMITED,    // 429
    TIMEOUT,         // request timed out
    CANCELLED,       // upstream or caller cancelled

    // Server-side problems
    UPSTREAM_ERROR,  // 5xx from dataset service
    INTERNAL_ERROR,   // unexpected error inside this service
    NETWORK_ERROR,
    UNKNOWN_ERROR;    // catch-all for unclassified errors
    public boolean isFinish() {
        return this == PENDING|| this == IN_PROGRESS ? false : true;

    }
    public boolean isSuccess() {
        return this == SUCCESS || this == EMPTY;
    }
    public boolean isFailure() {
        return this == INVALID_REQUEST ||
               this == UNAUTHORIZED ||
               this == FORBIDDEN ||
               this == NOT_FOUND ||
               this == RATE_LIMITED ||
               this == TIMEOUT ||
               this == CANCELLED ||
               this == UPSTREAM_ERROR ||
               this == INTERNAL_ERROR ||
               this == NETWORK_ERROR ||
               this == UNKNOWN_ERROR;
    }   
}
