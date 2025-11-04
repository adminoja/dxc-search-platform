package th.go.dxc.platform.search.config;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import th.go.dxc.platform.search.application.common.cache.port.out.CachePort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;

@Configuration
public class CacheConfig {
  private static Caffeine<Object, Object> build(CacheProperties.Item p) {
    Caffeine<Object, Object> c = Caffeine.newBuilder().maximumSize(p.maximumSize());
    if (p.expireAfterWrite() != null)
      c = c.expireAfterWrite(p.expireAfterWrite());
    if (p.expireAfterAccess() != null)
      c = c.expireAfterAccess(p.expireAfterAccess());
    return c;
  }

  @Bean
  Cache<String, Object> caffeineCache() {
    // Global TTL; adjust to your detailTtl()
    return Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build();
  }

  @Bean
  @Qualifier("reportTokenCache")
  public Cache<String, LocalSearchResult> reportTokenCache(CacheProperties props) {
    return build(props.reportToken()).build();
  }

  @Bean
  @Qualifier("globalSearchStatusCache")
  public Cache<String, GlobalSearchState> globalSearchStatusCache(CacheProperties props) {
    return build(props.globalSearch().status()).build();
  }

  @Bean
  @Qualifier("globalSearchResultCache")
  public Cache<String, GlobalSearchResult> globalSearchResultCache(CacheProperties props) {
    return build(props.globalSearch().result()).recordStats().build();
  }

  @Bean("globalSearchRunCache")
  public Cache<String, GlobalSearchRun> globalSearchRunCache(CacheProperties props) {
    return build(props.globalSearch().run()).build();
  }

  @Bean("userRunIndexCache")
  public Cache<String, Deque<String>> userRunIndexCache(CacheProperties props) {
    return build(props.globalSearch().userIndex()).build();
  }

  @Bean
  CachePort cachePort(Cache<String, Object> caffeineCache) {
    // This adapter ignores per-entry TTL; global TTL is enforced by cache config.
    return new CachePort() {
      @Override
      public <T> void put(String key, T value, java.time.Duration ttl) {
        caffeineCache.put(key, value);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> T get(String key, Class<T> type) {
        Object v = caffeineCache.getIfPresent(key);
        return v == null ? null : (T) v;
      }

      @Override
      public void evict(String key) {
        caffeineCache.invalidate(key);
      }
    };
  }
}
