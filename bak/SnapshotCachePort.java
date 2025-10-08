package th.go.dxc.platform.search.adapter.out.cache;
import java.time.Duration;

public interface SnapshotCachePort {
    /** Store a point-in-time snapshot and return an opaque, short-lived token. */
    String putSnapshot(String scope, Object snapshot, Duration ttl);

    /** (Used later by PDF flow) Resolve a snapshot by token; return null if expired/not found. */
    <T> T resolve(String cacheToken, Class<T> type);
}