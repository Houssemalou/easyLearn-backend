package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.document.DocumentAccessResponse;
import com.free.easyLearn.dto.document.LearningDocumentCommentDTO;
import com.free.easyLearn.dto.document.LearningDocumentDTO;
import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.LearningDocumentCommentService;
import com.free.easyLearn.service.LearningDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-documents")
@Tag(name = "Learning Documents", description = "Gestion des cours, devoirs et exercices")
@SecurityRequirement(name = "bearerAuth")
public class LearningDocumentController {

    private final LearningDocumentService learningDocumentService;
    private final LearningDocumentCommentService learningDocumentCommentService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public LearningDocumentController(LearningDocumentService learningDocumentService,
                                      LearningDocumentCommentService learningDocumentCommentService,
                                      UserRepository userRepository,
                                      StudentRepository studentRepository) {
        this.learningDocumentService = learningDocumentService;
        this.learningDocumentCommentService = learningDocumentCommentService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer une ressource", description = "Upload de cours, devoir ou exercice avec correction optionnelle")
    public ResponseEntity<ApiResponse<LearningDocumentDTO>> createDocument(
            @RequestParam String title,
            @RequestParam LearningDocument.DocumentCategory category,
            @RequestParam LearningDocument.DocumentSubject subject,
            @RequestParam Student.LanguageLevel level,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam MultipartFile file,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime correctionAvailableAt,
            @RequestParam(required = false) MultipartFile correctionFile
    ) {
        UUID userId = getAuthenticatedUserId();
        LearningDocumentDTO created = learningDocumentService.createDocument(
                userId,
                title,
                category,
                subject,
                level,
                description,
                isPublished,
                file,
                correctionAvailableAt,
                correctionFile
        );
        return ResponseEntity.ok(ApiResponse.success("Learning document created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mettre à jour une ressource")
    public ResponseEntity<ApiResponse<LearningDocumentDTO>> updateDocument(
            @PathVariable UUID id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LearningDocument.DocumentCategory category,
            @RequestParam(required = false) LearningDocument.DocumentSubject subject,
            @RequestParam(required = false) Student.LanguageLevel level,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) MultipartFile file,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime correctionAvailableAt,
            @RequestParam(required = false) MultipartFile correctionFile,
            @RequestParam(required = false) Boolean removeCorrection
    ) {
        UUID userId = getAuthenticatedUserId();
        LearningDocumentDTO updated = learningDocumentService.updateDocument(
                id,
                userId,
                title,
                category,
                subject,
                level,
                description,
                isPublished,
                file,
                correctionAvailableAt,
                correctionFile,
                removeCorrection
        );

        return ResponseEntity.ok(ApiResponse.success("Learning document updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Supprimer une ressource")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        learningDocumentService.deleteDocument(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Learning document deleted successfully", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mes ressources")
    public ResponseEntity<ApiResponse<List<LearningDocumentDTO>>> getMyDocuments() {
        UUID userId = getAuthenticatedUserId();
        List<LearningDocumentDTO> documents = learningDocumentService.getProfessorDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Ressources élève", description = "Ressources publiées selon niveau et organisation createdBy")
    public ResponseEntity<ApiResponse<List<LearningDocumentDTO>>> getStudentDocuments(
            @RequestParam(required = false) Student.LanguageLevel level,
            @RequestParam(required = false) LearningDocument.DocumentCategory category,
            @RequestParam(required = false) LearningDocument.DocumentSubject subject
    ) {
        UUID userId = getAuthenticatedUserId();
        List<LearningDocumentDTO> documents = learningDocumentService.getStudentDocuments(userId, level, category, subject);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'une ressource")
    public ResponseEntity<ApiResponse<LearningDocumentDTO>> getDocumentById(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        LearningDocumentDTO dto;
        if (user.getRole() == User.UserRole.PROFESSOR) {
            dto = learningDocumentService.getProfessorDocumentById(id, user.getId());
        } else if (user.getRole() == User.UserRole.STUDENT) {
            dto = learningDocumentService.getStudentDocumentById(id, user.getId());
        } else {
            throw new BadRequestException("Only professor and student can access this resource");
        }
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Ajouter un commentaire sur une ressource")
    public ResponseEntity<ApiResponse<LearningDocumentCommentDTO>> addComment(
            @PathVariable UUID id,
            @RequestParam String content
    ) {
        UUID userId = getAuthenticatedUserId();
        LearningDocumentCommentDTO created = learningDocumentCommentService.addComment(id, userId, content);
        return ResponseEntity.ok(ApiResponse.success("Comment added successfully", created));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les commentaires d'une ressource")
    public ResponseEntity<ApiResponse<List<LearningDocumentCommentDTO>>> getComments(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        List<LearningDocumentCommentDTO> comments = learningDocumentCommentService.getComments(id, user);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PutMapping("/{id}/comments/{commentId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Modifier un commentaire")
    public ResponseEntity<ApiResponse<LearningDocumentCommentDTO>> updateComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            @RequestParam String content
    ) {
        UUID userId = getAuthenticatedUserId();
        LearningDocumentCommentDTO updated = learningDocumentCommentService.updateComment(id, commentId, userId, content);
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", updated));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Supprimer un commentaire")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId
    ) {
        UUID userId = getAuthenticatedUserId();
        learningDocumentCommentService.deleteComment(id, commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    @PostMapping("/{id}/access")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Track document access", description = "Enregistre quand un élève ouvre un document")
    public ResponseEntity<ApiResponse<Void>> trackAccess(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        learningDocumentService.trackDocumentAccess(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Access tracked", null));
    }

    @GetMapping("/{id}/access")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Liste des élèves ayant accédé au document", description = "Pagination pour la liste des accès")
    public ResponseEntity<ApiResponse<DocumentAccessResponse>> getDocumentAccess(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        UUID userId = getAuthenticatedUserId();
        DocumentAccessResponse response = learningDocumentService.getDocumentAccess(id, userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getAuthenticatedUserId() {
        return getAuthenticatedUser().getId();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .or(() -> studentRepository.findByUniqueCode(username).map(Student::getUser))
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
