package lt.printerrental.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StorageExceptionHandler {
  @ExceptionHandler(AttachmentNotFoundException.class)
  ProblemDetail notFound(AttachmentNotFoundException error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
  }

  @ExceptionHandler(StorageUnavailableException.class)
  ProblemDetail unavailable(StorageUnavailableException error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage());
  }
}
