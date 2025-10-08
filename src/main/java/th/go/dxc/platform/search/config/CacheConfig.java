package th.go.dxc.platform.search.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import th.go.dxc.platform.search.application.common.cache.port.out.CachePort;

@Configuration
public class CacheConfig {

  @Bean
  Cache<String,Object> caffeineCache() {
    // Global TTL; adjust to your detailTtl()
    return Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build();
  }

  @Bean
  CachePort cachePort(Cache<String,Object> caffeineCache) {
    // This adapter ignores per-entry TTL; global TTL is enforced by cache config.
    return new CachePort() {
      @Override public <T> void put(String key, T value, java.time.Duration ttl) { caffeineCache.put(key, value); }
      @SuppressWarnings("unchecked")
      @Override public <T> T get(String key, Class<T> type) { Object v = caffeineCache.getIfPresent(key); return v==null?null:(T)v; }
      @Override public void evict(String key) { caffeineCache.invalidate(key); }
    };
  }
}
