package th.go.dxc.platform.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import th.go.dxc.platform.search.adapter.out.catalog.config.CatalogProperties;

@Configuration
@EnableConfigurationProperties({SearchCacheProperties.class, CatalogProperties.class, CorsProperties.class, SecurityProperties.class,ReportProperties.class})
public class AppConfig {}
