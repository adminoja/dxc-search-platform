package th.go.dxc.platform.search.application.search.port.out;

import java.util.Optional;

public interface SnapshotCachePort {
    /** Return the cached snapshot (point-in-time item) by opaque token, if valid & unexpired. */
    <T> Optional<T> resolve(String cacheToken, Class<T> type);
}