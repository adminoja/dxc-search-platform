package th.go.dxc.platform.search.application.common.cache.port.out;

import java.time.Duration;

public interface CachePort {
  <T> void put(String key, T value, Duration ttl);
  <T> T get(String key, Class<T> type);
  void evict(String key);
}