package com.free.easyLearn.service;

import com.free.easyLearn.dto.document.LearningDocumentDTO;
import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.LearningDocumentRepository;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LearningDocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "mp3", "mp4"
    );

    private final LearningDocumentRepository learningDocumentRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final MinioClient minioClient;

    @Value("${app.learning-documents.bucket:learning-documents}")
    private String learningDocumentsBucket;

    public LearningDocumentService(LearningDocumentRepository learningDocumentRepository,
                                   ProfessorRepository professorRepository,
                                   StudentRepository studentRepository,
                                   MinioClient minioClient) {
        this.learningDocumentRepository = learningDocumentRepository;
        this.professorRepository = professorRepository;
        this.studentRepository = studentRepository;
        this.minioClient = minioClient;
    }

    @Transactional
    public LearningDocumentDTO createDocument(UUID professorUserId,
                                              String title,
                                              LearningDocument.DocumentCategory category,
                                              LearningDocument.DocumentSubject subject,
                                              Student.LanguageLevel level,
                                              String description,
                                              Boolean isPublished,
                                              MultipartFile file,
                                              LocalDateTime correctionAvailableAt,
                                              MultipartFile correctionFile) {
        validateCreateInput(title, category, subject, level, file);
        validateCorrectionRulesForCategory(category, correctionFile, correctionAvailableAt);

        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        String mainObjectKey = buildObjectKey(category, subject, level, title, file.getOriginalFilename(), "main");
        uploadFile(mainObjectKey, file);

        String correctionObjectKey = null;
        if (correctionFile != null && !correctionFile.isEmpty()) {
            validateFileType(correctionFile);
            correctionObjectKey = buildObjectKey(category, subject, level, title, correctionFile.getOriginalFilename(), "correction");
            uploadFile(correctionObjectKey, correctionFile);
        }

        LearningDocument document = LearningDocument.builder()
                .title(title.trim())
                .category(category)
                .subject(subject)
                .level(level)
                .description(description)
                .fileName(file.getOriginalFilename())
                .objectKey(mainObjectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .correctionFileName(correctionFile != null ? correctionFile.getOriginalFilename() : null)
                .correctionObjectKey(correctionObjectKey)
                .correctionContentType(correctionFile != null ? correctionFile.getContentType() : null)
                .correctionFileSize(correctionFile != null ? correctionFile.getSize() : null)
                .correctionAvailableAt(correctionObjectKey != null ? correctionAvailableAt : null)
                .isPublished(isPublished == null || isPublished)
                .professor(professor)
                .build();

        LearningDocument saved = learningDocumentRepository.save(document);
        return mapToDTO(saved, true);
    }

    @Transactional
    public LearningDocumentDTO updateDocument(UUID documentId,
                                              UUID professorUserId,
                                              String title,
                                              LearningDocument.DocumentCategory category,
                                              LearningDocument.DocumentSubject subject,
                                              Student.LanguageLevel level,
                                              String description,
                                              Boolean isPublished,
                                              MultipartFile file,
                                              LocalDateTime correctionAvailableAt,
                                              MultipartFile correctionFile,
                                              Boolean removeCorrection) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        LearningDocument document = learningDocumentRepository.findByIdAndProfessorId(documentId, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (title != null) {
            if (title.isBlank()) {
                throw new BadRequestException("Title cannot be empty");
            }
            document.setTitle(title.trim());
        }
        if (category != null) {
            document.setCategory(category);
        }
        if (subject != null) {
            document.setSubject(subject);
        }
        if (level != null) {
            document.setLevel(level);
        }
        if (description != null) {
            document.setDescription(description);
        }
        if (isPublished != null) {
            document.setIsPublished(isPublished);
        }

        if (file != null && !file.isEmpty()) {
            validateFileType(file);
            deleteObjectQuietly(document.getObjectKey());
            String objectKey = buildObjectKey(
                    document.getCategory(),
                    document.getSubject(),
                    document.getLevel(),
                    document.getTitle(),
                    file.getOriginalFilename(),
                    "main"
            );
            uploadFile(objectKey, file);
            document.setFileName(file.getOriginalFilename());
            document.setObjectKey(objectKey);
            document.setContentType(file.getContentType());
            document.setFileSize(file.getSize());
        }

        boolean shouldRemoveCorrection = Boolean.TRUE.equals(removeCorrection);

        if (document.getCategory() == LearningDocument.DocumentCategory.COURSE) {
            if (correctionFile != null && !correctionFile.isEmpty()) {
                throw new BadRequestException("Course category cannot have a correction file");
            }
            if (correctionAvailableAt != null) {
                throw new BadRequestException("Course category cannot have correction availability date");
            }
            shouldRemoveCorrection = true;
        }

        if (shouldRemoveCorrection) {
            deleteObjectQuietly(document.getCorrectionObjectKey());
            document.setCorrectionFileName(null);
            document.setCorrectionObjectKey(null);
            document.setCorrectionContentType(null);
            document.setCorrectionFileSize(null);
            document.setCorrectionAvailableAt(null);
        }

        if (correctionFile != null && !correctionFile.isEmpty()) {
            validateFileType(correctionFile);
            deleteObjectQuietly(document.getCorrectionObjectKey());
            String correctionObjectKey = buildObjectKey(
                    document.getCategory(),
                    document.getSubject(),
                    document.getLevel(),
                    document.getTitle(),
                    correctionFile.getOriginalFilename(),
                    "correction"
            );
            uploadFile(correctionObjectKey, correctionFile);
            document.setCorrectionFileName(correctionFile.getOriginalFilename());
            document.setCorrectionObjectKey(correctionObjectKey);
            document.setCorrectionContentType(correctionFile.getContentType());
            document.setCorrectionFileSize(correctionFile.getSize());
            document.setCorrectionAvailableAt(correctionAvailableAt);
        } else if (correctionAvailableAt != null) {
            if (document.getCorrectionObjectKey() == null || document.getCorrectionObjectKey().isBlank()) {
                throw new BadRequestException("Attach a correction file before scheduling its availability");
            }
            document.setCorrectionAvailableAt(correctionAvailableAt);
        }

        LearningDocument updated = learningDocumentRepository.save(document);
        return mapToDTO(updated, true);
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID professorUserId) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        LearningDocument document = learningDocumentRepository.findByIdAndProfessorId(documentId, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        deleteObjectQuietly(document.getObjectKey());
        deleteObjectQuietly(document.getCorrectionObjectKey());
        learningDocumentRepository.delete(document);
    }

    public List<LearningDocumentDTO> getProfessorDocuments(UUID professorUserId) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        return learningDocumentRepository.findAllByProfessorIdOrderByCreatedAtDesc(professor.getId())
                .stream()
                .map(document -> mapToDTO(document, true))
                .toList();
    }

    public List<LearningDocumentDTO> getStudentDocuments(UUID studentUserId,
                                                         Student.LanguageLevel level,
                                                         LearningDocument.DocumentCategory category,
                                                         LearningDocument.DocumentSubject subject) {

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Student.LanguageLevel effectiveLevel = level != null ? level : student.getLevel();
        if (student.getCreatedBy() == null) {
            return List.of();
        }

        return learningDocumentRepository.findStudentDocuments(effectiveLevel, student.getCreatedBy().getId(), category, subject)
                .stream()
            .map(document -> mapToDTO(document, true, true))
                .toList();
    }

    public LearningDocumentDTO getProfessorDocumentById(UUID id, UUID professorUserId) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        LearningDocument document = learningDocumentRepository.findByIdAndProfessorId(id, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return mapToDTO(document, true);
    }

    public LearningDocumentDTO getStudentDocumentById(UUID id, UUID studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getCreatedBy() == null) {
            throw new ResourceNotFoundException("Document not found");
        }

        LearningDocument document = learningDocumentRepository
                .findStudentDocumentById(id, student.getLevel(), student.getCreatedBy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        return mapToDTO(document, true, true);
    }

    private void validateCreateInput(String title,
                                     LearningDocument.DocumentCategory category,
                                     LearningDocument.DocumentSubject subject,
                                     Student.LanguageLevel level,
                                     MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Document title is required");
        }
        if (category == null) {
            throw new BadRequestException("Document category is required");
        }
        if (subject == null) {
            throw new BadRequestException("Document subject is required");
        }
        if (subject == LearningDocument.DocumentSubject.OTHER) {
            throw new BadRequestException("Document subject is required");
        }
        if (level == null) {
            throw new BadRequestException("Document level is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file is required");
        }
        validateFileType(file);
    }

    private void validateCorrectionRulesForCategory(LearningDocument.DocumentCategory category,
                                                    MultipartFile correctionFile,
                                                    LocalDateTime correctionAvailableAt) {
        if (category == LearningDocument.DocumentCategory.COURSE) {
            if (correctionFile != null && !correctionFile.isEmpty()) {
                throw new BadRequestException("Course category cannot have a correction file");
            }
            if (correctionAvailableAt != null) {
                throw new BadRequestException("Course category cannot have correction availability date");
            }
        }

        if (correctionAvailableAt != null && (correctionFile == null || correctionFile.isEmpty())) {
            throw new BadRequestException("Attach a correction file before scheduling its availability");
        }
    }

    private void validateFileType(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot >= originalName.length() - 1) {
            throw new BadRequestException("File extension is required");
        }

        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type. Allowed: PDF, Office files, images, audio/video, txt, zip");
        }
    }

    private String sanitizeFolderName(String value, String fallback) {
        String safe = (value == null ? fallback : value)
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isBlank()) {
            return fallback;
        }
        return safe.toLowerCase(Locale.ROOT);
    }

    private String buildObjectKey(LearningDocument.DocumentCategory category,
                                  LearningDocument.DocumentSubject subject,
                                  Student.LanguageLevel level,
                                  String title,
                                  String originalFileName,
                                  String role) {
        String safeName = (originalFileName == null ? "document.bin" : originalFileName)
                .replace(" ", "_")
                .replace("/", "_")
                .replace("\\", "_");

        String folder = sanitizeFolderName(title, "document");
        String subjectFolder = subject != null ? subject.name().toLowerCase(Locale.ROOT) : "general";
        return "learning-documents/" + category.name().toLowerCase(Locale.ROOT) + "/" +
            subjectFolder + "/" +
                level.name().toLowerCase(Locale.ROOT) + "/" + folder + "/" +
                Instant.now().toEpochMilli() + "-" + role + "-" + safeName;
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(learningDocumentsBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(learningDocumentsBucket).build());
            }
        } catch (Exception e) {
            throw new BadRequestException("Unable to initialize learning documents bucket");
        }
    }

    private void uploadFile(String objectKey, MultipartFile file) {
        ensureBucketExists();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(learningDocumentsBucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build()
            );
        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new BadRequestException("Unable to upload learning document file");
        }
    }

    private void deleteObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(learningDocumentsBucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ignored) {
            // Best effort cleanup
        }
    }

    private String generatePresignedUrl(String objectKey, int minutes) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(learningDocumentsBucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(minutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private LearningDocumentDTO mapToDTO(LearningDocument document, boolean includeSignedUrl) {
        return mapToDTO(document, includeSignedUrl, false);
    }

    private LearningDocumentDTO mapToDTO(LearningDocument document, boolean includeSignedUrl, boolean restrictCorrectionForStudent) {
        boolean correctionVisible = !restrictCorrectionForStudent || isCorrectionVisibleForStudents(document);
        String fileUrl = includeSignedUrl ? generatePresignedUrl(document.getObjectKey(), 120) : null;
        String correctionFileUrl = includeSignedUrl && correctionVisible
                ? generatePresignedUrl(document.getCorrectionObjectKey(), 120)
                : null;

        return LearningDocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .category(document.getCategory())
                .subject(document.getSubject())
                .level(document.getLevel())
                .description(document.getDescription())
                .fileName(document.getFileName())
                .objectKey(document.getObjectKey())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .fileUrl(fileUrl)
                .correctionFileName(correctionVisible ? document.getCorrectionFileName() : null)
                .correctionObjectKey(correctionVisible ? document.getCorrectionObjectKey() : null)
                .correctionContentType(correctionVisible ? document.getCorrectionContentType() : null)
                .correctionFileSize(correctionVisible ? document.getCorrectionFileSize() : null)
                .correctionFileUrl(correctionFileUrl)
                .correctionAvailableAt(document.getCorrectionAvailableAt())
                .isPublished(document.getIsPublished())
                .professorId(document.getProfessor() != null ? document.getProfessor().getId() : null)
                .professorUserId(document.getProfessor() != null && document.getProfessor().getUser() != null
                        ? document.getProfessor().getUser().getId() : null)
                .professorName(document.getProfessor() != null && document.getProfessor().getUser() != null
                        ? document.getProfessor().getUser().getName() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private boolean isCorrectionVisibleForStudents(LearningDocument document) {
        if (document.getCategory() == LearningDocument.DocumentCategory.COURSE) {
            return false;
        }
        if (document.getCorrectionObjectKey() == null || document.getCorrectionObjectKey().isBlank()) {
            return false;
        }
        if (document.getCorrectionAvailableAt() == null) {
            return true;
        }
        return !LocalDateTime.now().isBefore(document.getCorrectionAvailableAt());
    }
}
