package lt.printerrental.storage;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
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

  @Test
  void rejectsDotTraversalFilenames() {
    var bytes = new byte[] {1, 2, 3};
    for (var name : List.of(".", "..", "dir/..", "dir\\.", " .. ")) {
      assertThatThrownBy(() -> new AttachmentUpload(name, bytes))
          .as(name)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void blocksDangerousExecutableAndActiveWebExtensionsCaseInsensitively() {
    var bytes = new byte[] {1, 2, 3};
    for (var name :
        List.of(
            "run.exe",
            "RUN.EXE",
            "script.bat",
            "script.cmd",
            "tool.com",
            "setup.msi",
            "screen.scr",
            "legacy.pif",
            "deploy.sh",
            ".sh",
            "deploy.PS1",
            "macro.vbs",
            "bundle.js",
            "module.mjs",
            "page.html",
            "PAGE.HTML",
            "page.htm",
            "doc.xhtml",
            "vector.SvG")) {
      assertThatThrownBy(() -> new AttachmentUpload(name, bytes))
          .as(name)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void allowsNormalDocumentImageTextAndArchiveFormats() {
    var bytes = new byte[] {1, 2, 3};
    for (var name :
        List.of(
            "contract.pdf",
            "notes.txt",
            "scan.png",
            "photo.JPG",
            "letter.doc",
            "contract.docx",
            "sheet.xls",
            "report.xlsx",
            "export.csv",
            "bundle.zip",
            "README")) {
      var upload = new AttachmentUpload(name, bytes);
      assertThat(upload.filename()).isEqualTo(name);
      assertThat(upload.size()).isEqualTo(3);
    }
    assertThat(new AttachmentUpload(null, bytes).filename()).isEqualTo("attachment");
  }
}
