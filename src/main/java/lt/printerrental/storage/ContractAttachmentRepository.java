package lt.printerrental.storage;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractAttachmentRepository extends JpaRepository<ContractAttachment, UUID> {
  Page<ContractAttachment> findByContractId(UUID contractId, Pageable pageable);

  Optional<ContractAttachment> findByIdAndContractId(UUID id, UUID contractId);
}
