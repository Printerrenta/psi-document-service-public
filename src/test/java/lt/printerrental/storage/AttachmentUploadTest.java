package lt.printerrental.storage;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttachmentUploadTest {
  @Test
  void callerCannotMutateValidatedUploadBytes() {
    var input = new byte[] {1, 2, 3};
    var upload = new AttachmentUpload("../test.txt", input);
    input[0] = 9;
    upload.bytes()[1] = 9;
    assertThat(upload.bytes()).containsExactly(1, 2, 3);
    assertThat(upload.filename()).isEqualTo("test.txt");
  }

  @Test
  void refusesEmptyOrOversizedCommandsWithoutAnyHttpDependency() {
    assertThatThrownBy(() -> new AttachmentUpload("empty", new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AttachmentUpload("large", new byte[5 * 1024 * 1024 + 1]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
