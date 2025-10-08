package th.go.dxc.platform.search.adapter.in.http.api.common.cache.adapter;


import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import th.go.dxc.platform.search.application.common.cache.port.out.CachePort;

public class CaffeineCacheAdapter implements CachePort {
  private final Cache<String, Object> cache;

  public CaffeineCacheAdapter(long maxSize) {
    this.cache = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .build();
  }

  @Override
  public <T> void put(String key, T value, Duration ttl) {
    // per-entry TTL: emulate with manual expiry by scheduling, or simplest: wrap key with TTL suffix.
    // For most cases, a single global expireAfterWrite works better:
    cache.put(key, value);
    // NOTE: if you need strict per-entry TTL, use Caffeine's Policy APIs or a small scheduler;
    // otherwise configure a global TTL (see config below).
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(String key, Class<T> type) {
    Object v = cache.getIfPresent(key);
    return (v == null) ? null : (T) v;
  }

  @Override
  public void evict(String key) {
    cache.invalidate(key);
  }
}
