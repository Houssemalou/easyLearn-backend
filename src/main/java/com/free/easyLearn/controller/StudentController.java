package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.common.PageResponse;
import com.free.easyLearn.dto.student.CreateStudentRequest;
import com.free.easyLearn.dto.student.StudentDTO;
import com.free.easyLearn.dto.student.UpdateStudentRequest;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students", description = "Gestion des étudiants")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mon profil étudiant", description = "Récupère le profil de l'étudiant connecté")
    public ResponseEntity<ApiResponse<StudentDTO>> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Students use uniqueCode as username
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> {
                    Student student = studentRepository.findByUniqueCode(username)
                            .orElseThrow(() -> new BadRequestException("User not found"));
                    return student.getUser();
                });

        StudentDTO student = studentService.getStudentByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved", student));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un étudiant", description = "Crée un nouvel étudiant avec compte utilisateur")
    public ResponseEntity<ApiResponse<StudentDTO>> createStudent(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        StudentDTO student = studentService.createStudent(request);
        return ResponseEntity.ok(ApiResponse.success("Student created successfully", student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @Operation(summary = "Modifier un étudiant", description = "Met à jour les informations d'un étudiant")
    public ResponseEntity<ApiResponse<StudentDTO>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request
    ) {
        StudentDTO student = studentService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un étudiant", description = "Supprime un étudiant et son compte utilisateur")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un étudiant", description = "Récupère les informations détaillées d'un étudiant")
    public ResponseEntity<ApiResponse<StudentDTO>> getStudentById(@PathVariable UUID id) {
        StudentDTO student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @GetMapping
    @Operation(summary = "Liste des étudiants", description = "Récupère la liste de tous les étudiants avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<StudentDTO>>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<StudentDTO> students = studentService.getStudents(page, size, sortBy, sortOrder);

        PageResponse<StudentDTO> response = PageResponse.<StudentDTO>builder()
                .data(students.getContent())
                .total(students.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(students.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/batch")
    @Operation(summary = "Récupérer plusieurs étudiants", description = "Récupère les infos de plusieurs étudiants par IDs")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getStudentsByIds(
            @RequestBody List<UUID> studentIds
    ) {
        List<StudentDTO> students = studentService.getStudentsByIds(studentIds);
        return ResponseEntity.ok(ApiResponse.success(students));
    }
}
