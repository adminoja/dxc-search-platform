package th.go.dxc.platform.search.domain.catalog.model;

import java.util.List;

public record Domain(
        Domain.Id id,
        String name,
        String description,
        List<String> canonicalKeys) {

    public record Id(String value) {
        public static Id of(String value) {
            return new Id(value);
        }
    }

    public static Domain of(Domain.Id id,
            String name,
            String description,
            List<String> canonicalKeys) {
        return new Domain(id, name, description, canonicalKeys);
    }
}
