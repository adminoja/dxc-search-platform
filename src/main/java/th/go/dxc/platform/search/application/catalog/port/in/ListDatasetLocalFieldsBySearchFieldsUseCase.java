package th.go.dxc.platform.search.application.catalog.port.in;

import java.util.List;
import java.util.Map;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;

public interface ListDatasetLocalFieldsBySearchFieldsUseCase {
    public Map<Dataset.Id,Map<String,String>> execute(Input input);
    public record Input(List<String> searchFields){
        public static Input of(List<String> searchFields){
            return new Input(searchFields);
        }
    }
}
