package th.go.dxc.platform.search.domain.search.model;

import java.util.List;

import th.go.dxc.platform.search.domain.catalog.model.Domain;
import th.go.dxc.platform.search.domain.catalog.model.Organization;

public record SpecializedReport(
        SpecializedReport.Id id,
        String name,
        Organization.Id ownerOrgId,
        List<Domain.Id> domainIds) {
    public record Id(String value) {
        public static Id of(String value) {
            return new Id(value);
        }

        public static SpecializedReport.Id empty() {
            return SpecializedReport.Id.of("");
        }
    }

    public static SpecializedReport of(SpecializedReport.Id id, String name, Organization.Id ownerOrgId,
            List<Domain.Id> domainIds) {
        id = id == null ? SpecializedReport.Id.of("") : id;
        name = name == null ? "" : name;
        ownerOrgId = ownerOrgId == null ? Organization.Id.of("") : ownerOrgId;
        domainIds = domainIds == null ? List.of() : domainIds;
        return new SpecializedReport(id, name, ownerOrgId, domainIds);
    }

}
