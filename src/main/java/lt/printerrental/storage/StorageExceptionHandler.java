package lt.printerrental.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badArgument(IllegalArgumentException error) {
    var message = error.getMessage();
    var detail = message == null || message.isBlank() ? "Invalid attachment request" : message;
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail payloadTooLarge(MaxUploadSizeExceededException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE, "Attachment exceeds maximum allowed size");
  }
}
