package th.go.dxc.platform.search.adapter.in.http.api.search.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class NotFoundAdvice {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoSuchElementException.class)
  public Map<String, Object> handleNoSuchElement(NoSuchElementException ex) {
    return Map.of(
        "error", "not_found",
        "message", ex.getMessage() == null ? "Resource not found" : ex.getMessage());
  }
}
