package th.go.dxc.platform.search.config;

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
                // @NotEmpty Map<String, @Valid DomainProps> domains,
                @NotEmpty List<@Valid OrganizationProps> organizations,
                @NotEmpty List<@Valid DatasetProps> datasets,
                List<@Valid DomainProps> domains,
                List<@Valid SpecializedReportProps> specializedReports) {
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
                        MappingProps mapping,
                        Map<String, Map<String, FieldRuleProps>> domains) {
        }

        public record RouteProps(
                        @NotBlank String path,
                        @NotBlank String serviceId,
                        Map<String, String> headers) {
        }

        public record MappingProps(
                        List<String> searchFields,
                        Map<String,String> canonicalSearchFields,
                        List<String> summaryFields,
                        List<String> naturalKeyFields,
                        String responseMapper) {
        }
        

        public record DomainProps(
                        String id,
                        String name,
                        String description,
                        java.util.List<String> canonicalKeys) {
        }

        public record SpecializedReportProps(
                        @NotBlank String id,
                        @NotBlank String name,
                        @NotBlank String ownerOrgId,
                        @NotEmpty List<String> domainIds) {
                
        }
        /** platform.catalog.datasets[*].domains.<domain> */
        // public record DatasetDomainPropsg(
        // String listPointer, // e.g. "/addresses" (inside recordRoot=/data). null =
        // one row per record
        // String datePointer, // e.g. "/updated_at" (inside each row/item)
        // Map<String, FieldRule> map // canonicalKey -> FieldRule
        // ) {
        // public DatasetDomainPropsg {
        // map = map == null ? new LinkedHashMap<>() : map;
        // }
        // }

        /** One of pointer | coalesce | compose; plus optional transforms */
        public record FieldRuleProps(
                        String pointer, // "/province_id"
                        List<String> coalesce, // ["/prov_code","/province_code"]
                        String compose, // "${/house_no} ${/road} จ.${/province_name}"
                        List<TransformRuleProps> transform // [{type:toDate}, {type:trim}]
        ) {
                public FieldRuleProps {
                        coalesce = coalesce == null ? List.of() : coalesce;
                        transform = transform == null ? List.of() : transform;
                }
        }

        public record TransformRuleProps(
                        TransformTypeProps type,
                        List<String> args) {
                public TransformRuleProps {
                        args = args == null ? List.of() : args;
                }

                public static TransformRuleProps of(TransformTypeProps type,
                                List<String> args) {
                        return new TransformRuleProps(type, args);
                }
        }

        public enum TransformTypeProps {
                toDate, trim, squashWhitespace, padLeft, toDecimal, mapCode, upper, lower
        }

}
