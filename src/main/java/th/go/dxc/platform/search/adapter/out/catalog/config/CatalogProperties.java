package th.go.dxc.platform.search.adapter.out.catalog.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "platform.catalog")
public record CatalogProperties(
                @NotEmpty List<@Valid OrganizationProps> organizations,
                @NotEmpty List<@Valid DatasetProps> datasets
                ) {
        public record OrganizationProps(
                        @NotBlank String id,
                        @NotBlank String name,
                        String description) {
        }

        public record DatasetProps(
                        @NotBlank String id,
                        @NotBlank String name,
                        String description,
                        @NotBlank String ownerOrgId,
                        RouteProps route,
                        MappingProps mapping) {
        }

        public record RouteProps(
                        @NotBlank String path,
                        @NotBlank String serviceId,
                        Map<String, String> headers) {
        }

        public record MappingProps(
                        List<String> searchFields,
                        List<String> summaryFields,
                        List<String> naturalKeyFields,
                        String responseMapper) {
        }
}
