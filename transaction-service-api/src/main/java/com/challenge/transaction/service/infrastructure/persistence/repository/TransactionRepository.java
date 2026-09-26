package com.challenge.transaction.service.infrastructure.persistence.repository;

import com.challenge.transaction.service.domain.TransactionStatus;
import com.challenge.transaction.service.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    boolean existsByReference(String reference);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
        update TransactionEntity t
           set t.status = :status
         where t.id = :id
           and t.reference = :reference
           and t.status = :expectedStatus
        """)
    int updateStatus(
            @Param("id") Long id,
            @Param("reference") String reference,
            @Param("status") TransactionStatus status,
            @Param("expectedStatus") TransactionStatus expectedStatus);
}
