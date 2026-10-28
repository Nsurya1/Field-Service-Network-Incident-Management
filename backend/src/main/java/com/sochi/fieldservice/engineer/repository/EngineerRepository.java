package com.sochi.fieldservice.engineer.repository;

import com.sochi.fieldservice.engineer.entity.Engineer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EngineerRepository extends JpaRepository<Engineer, Long> {

    Optional<Engineer> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Engineer e WHERE e.id = :id")
    Optional<Engineer> findByIdForUpdate(@Param("id") Long id);
}
