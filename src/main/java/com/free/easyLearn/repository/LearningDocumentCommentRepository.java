package com.free.easyLearn.repository;

import com.free.easyLearn.entity.LearningDocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningDocumentCommentRepository extends JpaRepository<LearningDocumentComment, UUID> {
    List<LearningDocumentComment> findAllByLearningDocumentIdOrderByCreatedAtDesc(UUID learningDocumentId);
}
