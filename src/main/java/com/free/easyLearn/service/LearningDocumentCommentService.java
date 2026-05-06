package com.free.easyLearn.service;

import com.free.easyLearn.dto.document.LearningDocumentCommentDTO;
import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.LearningDocumentComment;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.LearningDocumentCommentRepository;
import com.free.easyLearn.repository.LearningDocumentRepository;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LearningDocumentCommentService {

    private final LearningDocumentCommentRepository learningDocumentCommentRepository;
    private final LearningDocumentRepository learningDocumentRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;

    public LearningDocumentCommentService(LearningDocumentCommentRepository learningDocumentCommentRepository,
                                          LearningDocumentRepository learningDocumentRepository,
                                          ProfessorRepository professorRepository,
                                          StudentRepository studentRepository) {
        this.learningDocumentCommentRepository = learningDocumentCommentRepository;
        this.learningDocumentRepository = learningDocumentRepository;
        this.professorRepository = professorRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public LearningDocumentCommentDTO addComment(UUID documentId, UUID professorUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Comment content is required");
        }

        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        LearningDocument document = learningDocumentRepository.findByIdAndProfessorId(documentId, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        LearningDocumentComment comment = LearningDocumentComment.builder()
                .learningDocument(document)
                .professor(professor)
                .content(content.trim())
                .build();

        LearningDocumentComment saved = learningDocumentCommentRepository.save(comment);
        return mapToDTO(saved);
    }

    public List<LearningDocumentCommentDTO> getComments(UUID documentId, User user) {
        validateReadAccess(documentId, user);

        return learningDocumentCommentRepository.findAllByLearningDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

        @Transactional
        public LearningDocumentCommentDTO updateComment(UUID documentId,
                                                                                                        UUID commentId,
                                                                                                        UUID professorUserId,
                                                                                                        String content) {
                if (content == null || content.isBlank()) {
                        throw new BadRequestException("Comment content is required");
                }

                Professor professor = professorRepository.findByUserId(professorUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

                LearningDocumentComment comment = learningDocumentCommentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

                if (comment.getLearningDocument() == null || !comment.getLearningDocument().getId().equals(documentId)) {
                        throw new ResourceNotFoundException("Comment not found");
                }
                if (comment.getProfessor() == null || !comment.getProfessor().getId().equals(professor.getId())) {
                        throw new BadRequestException("You can only update your own comments");
                }

                comment.setContent(content.trim());
                LearningDocumentComment updated = learningDocumentCommentRepository.save(comment);
                return mapToDTO(updated);
        }

        @Transactional
        public void deleteComment(UUID documentId, UUID commentId, UUID professorUserId) {
                Professor professor = professorRepository.findByUserId(professorUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

                LearningDocumentComment comment = learningDocumentCommentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

                if (comment.getLearningDocument() == null || !comment.getLearningDocument().getId().equals(documentId)) {
                        throw new ResourceNotFoundException("Comment not found");
                }
                if (comment.getProfessor() == null || !comment.getProfessor().getId().equals(professor.getId())) {
                        throw new BadRequestException("You can only delete your own comments");
                }

                learningDocumentCommentRepository.delete(comment);
        }

    private void validateReadAccess(UUID documentId, User user) {
        if (user.getRole() == User.UserRole.PROFESSOR) {
            Professor professor = professorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

            learningDocumentRepository.findByIdAndProfessorId(documentId, professor.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
            return;
        }

        if (user.getRole() == User.UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

            if (student.getCreatedBy() == null) {
                throw new ResourceNotFoundException("Document not found");
            }

            learningDocumentRepository.findStudentDocumentById(
                            documentId,
                            student.getLevel(),
                            student.getCreatedBy().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
            return;
        }

        throw new BadRequestException("Only professor and student can access comments");
    }

    private LearningDocumentCommentDTO mapToDTO(LearningDocumentComment comment) {
        return LearningDocumentCommentDTO.builder()
                .id(comment.getId())
                .documentId(comment.getLearningDocument() != null ? comment.getLearningDocument().getId() : null)
                .professorId(comment.getProfessor() != null ? comment.getProfessor().getId() : null)
                .professorUserId(comment.getProfessor() != null && comment.getProfessor().getUser() != null
                        ? comment.getProfessor().getUser().getId() : null)
                .professorName(comment.getProfessor() != null && comment.getProfessor().getUser() != null
                        ? comment.getProfessor().getUser().getName() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
