package com.free.easyLearn.repository;

import com.free.easyLearn.entity.DocumentAccess;
import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, UUID> {

    Optional<DocumentAccess> findByDocumentAndStudent(LearningDocument document, Student student);

    Page<DocumentAccess> findByDocumentIdOrderByAccessedAtDesc(UUID documentId, Pageable pageable);

    @Query("SELECT da FROM DocumentAccess da WHERE da.document.id = :documentId ORDER BY da.accessedAt DESC")
    Page<DocumentAccess> findAccessByDocumentId(@Param("documentId") UUID documentId, Pageable pageable);

    @Query("SELECT COUNT(da) FROM DocumentAccess da WHERE da.document.id = :documentId")
    long countByDocumentId(@Param("documentId") UUID documentId);

    boolean existsByDocumentIdAndStudentId(UUID documentId, UUID studentId);
}
