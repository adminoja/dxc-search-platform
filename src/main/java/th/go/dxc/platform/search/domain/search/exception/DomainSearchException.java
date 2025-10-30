package th.go.dxc.platform.search.domain.search.exception;

import th.go.dxc.platform.search.domain.search.model.LocalSearchStatus;

// Domain exception that carries SearchStatus + optional HTTP status + code
public class DomainSearchException extends RuntimeException {
  private final LocalSearchStatus status;
  private final Integer httpStatus;
  private final String code;

  DomainSearchException(LocalSearchStatus status,
                        String message) {
    super(message);
    this.status = status; this.httpStatus = null; this.code = null;
  }
  public DomainSearchException(LocalSearchStatus status,
                        String message, Throwable cause,
                        Integer httpStatus, String code) {
    super(message, cause);
    this.status = status; this.httpStatus = httpStatus; this.code = code;
  }
  public LocalSearchStatus status() { return status; }
  public Integer httpStatus() { return httpStatus; }
  public String code() { return code; }
}
