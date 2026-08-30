package com.stitch.story.backend.repositories;

import com.stitch.story.backend.entities.PendingPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingPaymentRepository extends JpaRepository<PendingPayment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingPayment p JOIN FETCH p.user WHERE p.transactionUuid = :transactionUuid")
    Optional<PendingPayment> findByTransactionUuid(@Param("transactionUuid") String transactionUuid);
}
