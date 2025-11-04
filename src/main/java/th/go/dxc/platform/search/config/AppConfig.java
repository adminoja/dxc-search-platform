package th.go.dxc.platform.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SearchCacheProperties.class,CacheProperties.class, CatalogProperties.class, CorsProperties.class, SecurityProperties.class,ReportProperties.class})
public class AppConfig {}
