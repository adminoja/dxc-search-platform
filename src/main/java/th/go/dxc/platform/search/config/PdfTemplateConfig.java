package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;

@Configuration
public class PdfTemplateConfig implements WebFluxConfigurer {

  @Bean
  SpringResourceTemplateResolver pdfTemplateResolver() {
    var r = new SpringResourceTemplateResolver();
    r.setPrefix("classpath:/report/templates/pdf/");
    r.setSuffix(".html");
    r.setTemplateMode(TemplateMode.HTML);
    r.setCharacterEncoding("UTF-8");
    r.setCacheable(false);      // enable in prod
    r.setCheckExistence(true);
    r.setOrder(5);              // won’t override your default resolver
    r.setResolvablePatterns(Set.of("*/index", "*/partials/*"));
    return r;
  }

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/report-assets/**")
            .addResourceLocations("classpath:/report/templates/pdf/");
  }
}
