package th.go.dxc.platform.search.domain.catalog.model;

public record DomainField(
    Id id,
    DomainField.Type type,
    String description,
    String format
) {
    public record Id(String value) {
        public static Id of(String value) {
            return new Id(value);
        }
    }   

    public enum Type {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME,
        OBJECT,
        ARRAY
    }
}
