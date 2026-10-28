
package com.sochi.fieldservice.engineer.repository;

import com.sochi.fieldservice.engineer.entity.Engineer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EngineerRepository
        extends JpaRepository<Engineer, Long> {

    Optional<Engineer> findByEmail(String email);

    boolean existsByEmail(String email);
}
