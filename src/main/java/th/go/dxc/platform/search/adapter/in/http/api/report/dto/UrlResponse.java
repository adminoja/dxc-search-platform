package th.go.dxc.platform.search.adapter.in.http.api.report.dto;

public record UrlResponse(
    String url,
    String objectName,
    String contentType,
    int    expiresInSeconds
) {}
