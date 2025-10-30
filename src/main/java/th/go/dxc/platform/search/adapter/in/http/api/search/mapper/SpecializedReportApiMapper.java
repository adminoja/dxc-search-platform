package th.go.dxc.platform.search.adapter.in.http.api.search.mapper;

import java.time.Instant;
import java.util.Objects;

import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.PageRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SpecializedReportRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SpecializedReportResultDto;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainSort;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportRequest;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportResult;

public final class SpecializedReportApiMapper {

  private SpecializedReportApiMapper() {
  }

  public static SpecializedReportRequest toDomain(SpecializedReportRequestDto dto) {
    Objects.requireNonNull(dto, "dto");
    if (dto.reportId() == null || dto.reportId().isBlank() || dto.reportId().isEmpty()) {
      throw new IllegalArgumentException("'requests' must not be empty");
    }
    var now = Instant.now();
    // Pass sharedCriteria through because your domain model includes it
    return SpecializedReportRequest.of(now,SpecializedReport.Id.of(dto.reportId()) , dto.criteria(),toDomain( dto.pageRequest()));
  }



  public static SpecializedReportResultDto toDto(SpecializedReportResult domain) {
    Objects.requireNonNull(domain, "domain");

    return SpecializedReportResultDto.of(domain.runId(), domain.reportId(), domain.domainResults(), domain.startedAt(),domain.durationMs());
  }
  // ---- helpers ----

  private static DomainPageRequest toDomain(PageRequestDto pageRequestDto)
  {
    return DomainPageRequest.of(pageRequestDto.page(),pageRequestDto.size(),DomainSort.of(  pageRequestDto.sort()));
  }
}
