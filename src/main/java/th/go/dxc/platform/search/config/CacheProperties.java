package th.go.dxc.platform.search.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.cache")
public record CacheProperties(
    Item reportToken,
    GlobalSearch globalSearch
) {
  public CacheProperties {
    reportToken  = reportToken  == null ? Item.defaults() : reportToken.withDefaults();
    globalSearch = globalSearch == null ? GlobalSearch.defaults() : globalSearch.withDefaults();
  }

  public record Item(
      long maximumSize,
      Duration expireAfterWrite,
      Duration expireAfterAccess
  ) {
    public static Item defaults() {
      return new Item(10_000, null, null);
    }
    public Item withDefaults() {
      // ensure positive size and leave Durations as provided (can be null)
      long size = maximumSize > 0 ? maximumSize : 10_000;
      return new Item(size, expireAfterWrite, expireAfterAccess);
    }
  }

  public record GlobalSearch(
      Item status,
      Item result,
      Item run,
      Item userIndex
  ) {
    public static GlobalSearch defaults() {
      return new GlobalSearch(
          new Item(100_000, Duration.ofDays(30), null), // status
          new Item(10_000,  Duration.ofDays(30), null), // result
          new Item(10_000,  Duration.ofDays(180), null),// run
          new Item(10_000,  Duration.ofDays(180), null) // userIndex
      );
    }
    public GlobalSearch withDefaults() {
      return new GlobalSearch(
          status   == null ? defaults().status()   : status.withDefaults(),
          result   == null ? defaults().result()   : result.withDefaults(),
          run      == null ? defaults().run()      : run.withDefaults(),
          userIndex== null ? defaults().userIndex(): userIndex.withDefaults()
      );
    }
  }
}