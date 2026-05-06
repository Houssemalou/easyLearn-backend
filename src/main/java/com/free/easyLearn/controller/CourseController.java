package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.course.CourseDTO;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Gestion des cours et supports")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public CourseController(CourseService courseService,
                            UserRepository userRepository,
                            StudentRepository studentRepository) {
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer un cours", description = "Upload d'un cours avec son fichier support")
    public ResponseEntity<ApiResponse<CourseDTO>> createCourse(
            @RequestParam String name,
            @RequestParam Student.LanguageLevel level,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> files,
            @RequestParam(required = false) MultipartFile file
    ) {
        UUID professorUserId = getAuthenticatedUserId();
        List<MultipartFile> effectiveFiles = mergeFiles(files, file);
        CourseDTO created = courseService.createCourse(professorUserId, name, level, description, effectiveFiles);
        return ResponseEntity.ok(ApiResponse.success("Course created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mettre à jour un cours")
    public ResponseEntity<ApiResponse<CourseDTO>> updateCourse(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Student.LanguageLevel level,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> files,
            @RequestParam(required = false) MultipartFile file
    ) {
        UUID professorUserId = getAuthenticatedUserId();
        List<MultipartFile> effectiveFiles = mergeFiles(files, file);
        CourseDTO updated = courseService.updateCourse(id, professorUserId, name, level, description, effectiveFiles);
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Supprimer un cours")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable UUID id) {
        UUID professorUserId = getAuthenticatedUserId();
        courseService.deleteCourse(id, professorUserId);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mes cours")
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getMyCourses() {
        UUID professorUserId = getAuthenticatedUserId();
        List<CourseDTO> courses = courseService.getProfessorCourses(professorUserId);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/by-level")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cours par niveau", description = "Retourne les cours adaptés au niveau de l'élève")
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getCoursesByLevel(
            @RequestParam(required = false) Student.LanguageLevel level
    ) {
        Student.LanguageLevel effectiveLevel = level;
        if (effectiveLevel == null) {
            UUID userId = getAuthenticatedUserId();
            Student student = studentRepository.findByUserId(userId)
                    .orElseThrow(() -> new BadRequestException("Student profile not found"));
            effectiveLevel = student.getLevel();
        }

        List<CourseDTO> courses = courseService.getCoursesByLevel(effectiveLevel);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un cours")
    public ResponseEntity<ApiResponse<CourseDTO>> getCourseById(@PathVariable UUID id) {
        CourseDTO course = courseService.getCourseById(id, true);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return user.getId();
    }

    private List<MultipartFile> mergeFiles(List<MultipartFile> files, MultipartFile singleFile) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            merged.addAll(files.stream().filter(f -> f != null && !f.isEmpty()).toList());
        }
        if (singleFile != null && !singleFile.isEmpty()) {
            merged.add(singleFile);
        }
        return merged;
    }
}
