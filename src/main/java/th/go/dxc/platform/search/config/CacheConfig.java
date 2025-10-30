package th.go.dxc.platform.search.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import th.go.dxc.platform.search.application.common.cache.port.out.CachePort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;

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
  @Bean @Qualifier("reportTokenCache")
  public Cache<String, LocalSearchResult> reportTokenCache() {
    return Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterAccess(Duration.ofMinutes(30))   // token stays alive while being read
        .removalListener((String k, LocalSearchResult v, RemovalCause c) -> { /* optional cleanup */})
        .build();
  }

  @Bean @Qualifier("globalSearchStatusCache")
  public Cache<String, GlobalSearchState> globalSearchStatusCache() {
    return Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(Duration.ofMinutes(20))    // old runs drop out quickly
        .build();
  }

  @Bean @Qualifier("globalSearchResultCache")
  public Cache<String, GlobalSearchResult> globalSearchResultCache() {
    return Caffeine.newBuilder()
        .maximumSize(10_000)                         // results can be big
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats()                               // optional: monitor hit rate
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
