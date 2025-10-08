package th.go.dxc.platform.search.application.report.model;

public record RenderedReport(
    String filename,     // e.g., my-report.pdf
    String contentType,  // mime
    byte[] bytes
) {}
