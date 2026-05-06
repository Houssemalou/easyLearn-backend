package com.free.easyLearn.service;

import com.free.easyLearn.dto.course.CourseDTO;
import com.free.easyLearn.entity.Course;
import com.free.easyLearn.entity.CourseMaterial;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.CourseRepository;
import com.free.easyLearn.repository.ProfessorRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CourseService {

    private static final int MAX_FILES_PER_COURSE = 3;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private final CourseRepository courseRepository;
    private final ProfessorRepository professorRepository;
    private final MinioClient minioClient;

    @Value("${app.courses.bucket:courses}")
    private String coursesBucket;

    public CourseService(CourseRepository courseRepository,
                         ProfessorRepository professorRepository,
                         MinioClient minioClient) {
        this.courseRepository = courseRepository;
        this.professorRepository = professorRepository;
        this.minioClient = minioClient;
    }

    @Transactional
    public CourseDTO createCourse(UUID professorUserId,
                                  String name,
                                  Student.LanguageLevel level,
                                  String description,
                      List<MultipartFile> files) {
        List<MultipartFile> effectiveFiles = normalizeFiles(files);
        validateInput(name, level, effectiveFiles, true);

        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        List<CourseMaterial> materials = buildAndUploadMaterials(name, effectiveFiles);
        CourseMaterial primary = materials.get(0);

        Course course = Course.builder()
                .name(name.trim())
                .level(level)
                .description(description)
            .fileName(primary.getFileName())
            .objectKey(primary.getObjectKey())
            .contentType(primary.getContentType())
            .fileSize(primary.getFileSize())
                .professor(professor)
                .build();
        course.replaceMaterials(materials);

        Course saved = courseRepository.save(course);
        return mapToDTO(saved, true);
    }

    @Transactional
    public CourseDTO updateCourse(UUID courseId,
                                  UUID professorUserId,
                                  String name,
                                  Student.LanguageLevel level,
                                  String description,
                      List<MultipartFile> files) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        Course course = courseRepository.findByIdAndProfessorId(courseId, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<MultipartFile> effectiveFiles = normalizeFiles(files);
        validateInput(name, level, effectiveFiles, false);

        if (name != null && !name.isBlank()) {
            course.setName(name.trim());
        }
        if (level != null) {
            course.setLevel(level);
        }
        if (description != null) {
            course.setDescription(description);
        }

        if (!effectiveFiles.isEmpty()) {
            List<CourseMaterial> oldMaterials = getSortedMaterials(course);
            oldMaterials.forEach(material -> deleteObjectQuietly(material.getObjectKey()));
            if (course.getObjectKey() != null && !course.getObjectKey().isBlank()) {
                Set<String> oldKeys = oldMaterials.stream().map(CourseMaterial::getObjectKey).collect(java.util.stream.Collectors.toSet());
                if (!oldKeys.contains(course.getObjectKey())) {
                    deleteObjectQuietly(course.getObjectKey());
                }
            }

            List<CourseMaterial> newMaterials = buildAndUploadMaterials(course.getName(), effectiveFiles);
            CourseMaterial primary = newMaterials.get(0);
            course.replaceMaterials(newMaterials);

            course.setObjectKey(primary.getObjectKey());
            course.setFileName(primary.getFileName());
            course.setContentType(primary.getContentType());
            course.setFileSize(primary.getFileSize());
        }

        Course updated = courseRepository.save(course);
        return mapToDTO(updated, true);
    }

    @Transactional
    public void deleteCourse(UUID courseId, UUID professorUserId) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        Course course = courseRepository.findByIdAndProfessorId(courseId, professor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        getSortedMaterials(course).forEach(material -> deleteObjectQuietly(material.getObjectKey()));
        deleteObjectQuietly(course.getObjectKey());
        courseRepository.delete(course);
    }

    public List<CourseDTO> getProfessorCourses(UUID professorUserId) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        return courseRepository.findAllByProfessorIdOrderByCreatedAtDesc(professor.getId())
                .stream()
                .map(course -> mapToDTO(course, true))
                .toList();
    }

    public List<CourseDTO> getCoursesByLevel(Student.LanguageLevel level) {
        return courseRepository.findAllByLevelOrderByCreatedAtDesc(level)
                .stream()
                .map(course -> mapToDTO(course, false))
                .toList();
    }

    public CourseDTO getCourseById(UUID courseId, boolean includeSignedUrl) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return mapToDTO(course, includeSignedUrl);
    }

    public Course getCourseEntityById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }

    private void validateInput(String name,
                               Student.LanguageLevel level,
                               List<MultipartFile> files,
                               boolean fileRequired) {
        if (name != null && name.isBlank()) {
            throw new BadRequestException("Course name cannot be empty");
        }
        if (fileRequired && (name == null || name.isBlank())) {
            throw new BadRequestException("Course name is required");
        }
        if (fileRequired && level == null) {
            throw new BadRequestException("Course level is required");
        }

        if (fileRequired && files.isEmpty()) {
            throw new BadRequestException("At least one course file is required");
        }
        if (files.size() > MAX_FILES_PER_COURSE) {
            throw new BadRequestException("Maximum 3 files are allowed per course");
        }

        for (MultipartFile file : files) {
            validateFileType(file);
        }
    }

    private void validateFileType(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.contains(extension);
        boolean contentTypeAllowed = ALLOWED_CONTENT_TYPES.contains(contentType);

        if (!extensionAllowed && !contentTypeAllowed) {
            throw new BadRequestException("Only PDF, PNG and JPEG files are supported");
        }
    }

    private String sanitizeFolderName(String courseName) {
        String safe = (courseName == null ? "course" : courseName)
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isBlank()) {
            return "course";
        }
        return safe.toLowerCase(Locale.ROOT);
    }

    private String buildObjectKey(String courseName, String originalFileName, int index) {
        String safeName = (originalFileName == null ? "course.bin" : originalFileName)
                .replace(" ", "_")
                .replace("/", "_")
                .replace("\\", "_");
        String folder = sanitizeFolderName(courseName);
        return "courses/" + folder + "/" + Instant.now().toEpochMilli() + "-" + index + "-" + safeName;
    }

    private List<CourseMaterial> buildAndUploadMaterials(String courseName, List<MultipartFile> files) {
        List<CourseMaterial> materials = new ArrayList<>();
        int i = 0;
        for (MultipartFile file : files) {
            String objectKey = buildObjectKey(courseName, file.getOriginalFilename(), i);
            uploadFile(objectKey, file);
            CourseMaterial material = CourseMaterial.builder()
                    .fileName(file.getOriginalFilename())
                    .objectKey(objectKey)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .positionIndex(i)
                    .build();
            materials.add(material);
            i++;
        }
        return materials;
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(coursesBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(coursesBucket).build());
            }
        } catch (Exception e) {
            throw new BadRequestException("Unable to initialize courses bucket");
        }
    }

    private void uploadFile(String objectKey, MultipartFile file) {
        ensureBucketExists();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(coursesBucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build()
            );
        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new BadRequestException("Unable to upload course file");
        }
    }

    private void deleteObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(coursesBucket)
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
                            .bucket(coursesBucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(minutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private List<CourseMaterial> getSortedMaterials(Course course) {
        if (course.getMaterials() == null) {
            return List.of();
        }
        return course.getMaterials().stream()
                .sorted(Comparator.comparingInt(CourseMaterial::getPositionIndex))
                .toList();
    }

    private CourseDTO mapToDTO(Course course, boolean includeSignedUrl) {
        List<CourseMaterial> materials = getSortedMaterials(course);
        List<String> fileNames = new ArrayList<>();
        List<String> objectKeys = new ArrayList<>();

        if (!materials.isEmpty()) {
            materials.forEach(material -> {
                fileNames.add(material.getFileName());
                objectKeys.add(material.getObjectKey());
            });
        } else if (course.getObjectKey() != null && !course.getObjectKey().isBlank()) {
            fileNames.add(course.getFileName());
            objectKeys.add(course.getObjectKey());
        }

        List<String> fileUrls = includeSignedUrl
                ? objectKeys.stream().map(key -> generatePresignedUrl(key, 60)).toList()
                : List.of();

        String primaryObjectKey = objectKeys.isEmpty() ? course.getObjectKey() : objectKeys.get(0);
        String primaryFileName = fileNames.isEmpty() ? course.getFileName() : fileNames.get(0);
        String primaryFileUrl = includeSignedUrl ? (fileUrls.isEmpty() ? generatePresignedUrl(primaryObjectKey, 60) : fileUrls.get(0)) : null;

        return CourseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .level(course.getLevel())
                .description(course.getDescription())
                .fileName(primaryFileName)
                .objectKey(primaryObjectKey)
                .fileSize(course.getFileSize())
                .contentType(course.getContentType())
                .professorId(course.getProfessor() != null ? course.getProfessor().getId() : null)
                .fileUrl(primaryFileUrl)
                .fileNames(fileNames)
                .objectKeys(objectKeys)
                .fileUrls(fileUrls)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
