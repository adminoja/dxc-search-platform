package th.go.dxc.platform.search.application.search.port.out;

import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;

public interface LocalSearchStorePort {
    /** Store the local search result under a token for later retrieval. */
    void store(String token, LocalSearchResult localSearchResult, java.time.Duration ttl);
    
    /** Retrieve the local search result by token, if still valid. */
    LocalSearchResult resolve(String token);
}
