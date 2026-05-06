package com.free.easyLearn.repository;

import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningDocumentRepository extends JpaRepository<LearningDocument, UUID> {

    List<LearningDocument> findAllByProfessorIdOrderByCreatedAtDesc(UUID professorId);

    Optional<LearningDocument> findByIdAndProfessorId(UUID id, UUID professorId);

    @Query("SELECT d FROM LearningDocument d " +
            "WHERE d.isPublished = true " +
            "AND d.level = :level " +
            "AND d.professor.createdBy.id = :createdById " +
            "AND (:category IS NULL OR d.category = :category) " +
            "AND (:subject IS NULL OR d.subject = :subject) " +
            "ORDER BY d.createdAt DESC")
    List<LearningDocument> findStudentDocuments(
            @Param("level") Student.LanguageLevel level,
            @Param("createdById") UUID createdById,
            @Param("category") LearningDocument.DocumentCategory category,
            @Param("subject") LearningDocument.DocumentSubject subject
    );

    @Query("SELECT d FROM LearningDocument d " +
            "WHERE d.id = :id " +
            "AND d.isPublished = true " +
            "AND d.level = :level " +
            "AND d.professor.createdBy.id = :createdById")
    Optional<LearningDocument> findStudentDocumentById(
            @Param("id") UUID id,
            @Param("level") Student.LanguageLevel level,
            @Param("createdById") UUID createdById
    );
}
