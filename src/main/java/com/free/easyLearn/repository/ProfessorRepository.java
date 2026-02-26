package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Professor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

    Optional<Professor> findByUserId(UUID userId);

    @Query("SELECT p FROM Professor p ORDER BY p.user.name ASC")
    Page<Professor> findAllProfessors(Pageable pageable);

    @Query("SELECT p FROM Professor p JOIN p.createdBy c WHERE c.id = :adminId ORDER BY p.user.name ASC")
    Page<Professor> findAllProfessorsByAdmin(java.util.UUID adminId, Pageable pageable);
}
