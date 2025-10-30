package th.go.dxc.platform.search.application.catalog.port.in;

import java.util.Map;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Domain;

public interface ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase {
    public Map<Dataset.Id,Dataset.FieldRule> execute(Input input);
    public record Input(
        Domain.Id domainId,String canonicalKey
    ) {
        public static Input of(
            Domain.Id domainId,String canonicalKey
        ) {
            return new Input(domainId,canonicalKey);
        }
    }
}
