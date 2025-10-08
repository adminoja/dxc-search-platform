// src/main/java/th/go/dxc/platform/search/application/report/model/PresignedUrl.java
package th.go.dxc.platform.search.application.report.model;

public record PresignedUrl(
    String url,
    String objectName,
    String contentType,
    int    expiresInSeconds
) {}
