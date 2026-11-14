package com.banking.ledger.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> { }
