package lt.printerrental.storage;

import java.util.UUID;

/** Access contract used by services that operate on a contract's child resources. */
public interface ContractAccess {
  void requireRead(UUID contractId);

  void requireWrite(UUID contractId);
}
