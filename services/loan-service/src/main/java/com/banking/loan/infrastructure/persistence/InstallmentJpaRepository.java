package com.banking.loan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentJpaRepository extends JpaRepository<InstallmentEntity, UUID> {
    List<InstallmentEntity> findByLoanIdOrderByInstallmentNumber(UUID loanId);
    java.util.Optional<InstallmentEntity> findByLoanIdAndInstallmentNumber(UUID loanId,int installmentNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InstallmentEntity i where i.status in ('SCHEDULED','OVERDUE') and i.dueDate <= :today order by i.dueDate,i.installmentNumber")
    List<InstallmentEntity> findDueForUpdate(@Param("today") LocalDate today);
}
