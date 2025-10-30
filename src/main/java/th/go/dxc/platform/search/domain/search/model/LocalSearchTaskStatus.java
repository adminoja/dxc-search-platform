package th.go.dxc.platform.search.domain.search.model;

public record LocalSearchTaskStatus(String datasetId, LocalSearchStatus state, String message, Integer httpStatus,
        Long latencyMs, boolean truncated) {
}
