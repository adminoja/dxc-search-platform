package th.go.dxc.platform.search.domain.catalog.model;

import java.util.List;
import java.util.Map;

import lombok.ToString;

public record Dataset(Dataset.Id id,
        String name,
        String description,
        Organization.Id ownerOrgId,
        Route route,
        FieldMapping mapping,
        Map<String, Map<String, FieldRule>> domains) {

    public record Id(String value) {
        public static Id of(String value) {
            return new Id(value);
        }
    }

    public record Route(
            String path,
            String serviceId,
            Map<String, String> headers) {

    }

    public record FieldMapping(
        List<String> searchFields,
        Map<String,String> canonicalSearchFields,
        List<String> summaryFields,
        List<String> naturalKeyFields) {
    }
    
    public record FieldRule(
            String pointer, // "/province_id"
            List<String> coalesce, // ["/prov_code","/province_code"]
            String compose, // "${/house_no} ${/road} จ.${/province_name}"
            List<TransformRule> transform // [{type:toDate}, {type:trim}]
    ) {
        public FieldRule {
            coalesce = coalesce == null ? List.of() : coalesce;
            transform = transform == null ? List.of() : transform;
        }

        public static FieldRule of(String pointer,
                List<String> coalesce,
                String compose,
                List<TransformRule> transform) {
            return new FieldRule(pointer, coalesce, compose, transform);
        }
    }

    public record TransformRule(
            TransformType type,
            List<String> args) {
        public TransformRule {
            args = args == null ? List.of() : args;
        }
    }

    public enum TransformType {
        toDate, trim, squashWhitespace, padLeft, toDecimal, mapCode, upper, lower
    }

}
