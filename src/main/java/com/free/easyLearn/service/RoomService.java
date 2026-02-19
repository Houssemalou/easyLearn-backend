package com.free.easyLearn.service;

import com.free.easyLearn.dto.room.*;
import com.free.easyLearn.entity.*;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository participantRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private LiveKitService liveKitService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public RoomDTO createRoom(CreateRoomRequest request) {
        Room room = Room.builder()
                .name(request.getName())
                .language(request.getLanguage())
                .level(request.getLevel())
                .objective(request.getObjective())
                .scheduledAt(request.getScheduledAt())
                .duration(request.getDuration())
                .maxStudents(request.getMaxStudents())
                .status(Room.RoomStatus.SCHEDULED)
                .animatorType(request.getAnimatorType())
                .build();

        // Set professor if provided
        if (request.getProfessorId() != null) {
            Professor professor = professorRepository.findById(request.getProfessorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
            room.setProfessor(professor);
        }

        // Generate LiveKit room name
        room.setLivekitRoomName("room-" + UUID.randomUUID().toString());

        room = roomRepository.save(room);

        // Add invited students as participants
        if (request.getInvitedStudents() != null && !request.getInvitedStudents().isEmpty()) {
            for (UUID studentId : request.getInvitedStudents()) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

                RoomParticipant participant = RoomParticipant.builder()
                        .room(room)
                        .student(student)
                        .invited(true)
                        .build();

                participantRepository.save(participant);
            }
        }
        return mapToDTO(room);
    }

    @Transactional
    public RoomDTO updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (request.getName() != null) room.setName(request.getName());
        if (request.getObjective() != null) room.setObjective(request.getObjective());
        if (request.getScheduledAt() != null) room.setScheduledAt(request.getScheduledAt());
        if (request.getDuration() != null) room.setDuration(request.getDuration());
        if (request.getMaxStudents() != null) room.setMaxStudents(request.getMaxStudents());
        if (request.getStatus() != null) room.setStatus(request.getStatus());

        room = roomRepository.save(room);
        return mapToDTO(room);
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getLivekitRoomName() != null) {
            liveKitService.deleteLiveKitRoom(room.getLivekitRoomName());
        }

        roomRepository.delete(room);
    }

    public RoomDTO getRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return mapToDTO(room);
    }

    public RoomDTO getRoomByLivekitName(String livekitRoomName) {
        Room room = roomRepository.findByLivekitRoomName(livekitRoomName)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with LiveKit name: " + livekitRoomName));
        return mapToDTO(room);
    }

    public Page<RoomDTO> getRooms(int page, int size, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Room> rooms = roomRepository.findAllRooms(pageable);

        return rooms.map(this::mapToDTO);
    }

    public Page<RoomDTO> getMyRooms(UUID userId, User.UserRole userRole, int page, int size, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Room> rooms;

        if (userRole == User.UserRole.PROFESSOR) {
            // Get rooms where user is the assigned professor
            Professor professor = professorRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professor profile not found"));
            rooms = roomRepository.findByProfessorId(professor.getId(), pageable);
        } else if (userRole == User.UserRole.STUDENT) {
            // Get rooms where user is invited as student
            Student student = studentRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
            rooms = roomRepository.findByStudentId(student.getId(), pageable);
        } else {
            // Admins can see all rooms
            rooms = roomRepository.findAllRooms(pageable);
        }

        return rooms.map(this::mapToDTO);
    }

    @Transactional
    public ParticipantDTO muteParticipant(ParticipantActionRequest request) {
        RoomParticipant participant = participantRepository
                .findByRoomIdAndStudentId(request.getRoomId(), request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setIsMuted(request.getMuted());
        participant = participantRepository.save(participant);

        // Notify via LiveKit
        Room room = participant.getRoom();
        if (room.getLivekitRoomName() != null) {
            // Send data message to participant via LiveKit
            // This will be handled by the LiveKit service
        }

        return mapParticipantToDTO(participant);
    }

    @Transactional
    public ParticipantDTO pingParticipant(ParticipantActionRequest request) {
        RoomParticipant participant = participantRepository
                .findByRoomIdAndStudentId(request.getRoomId(), request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setIsPinged(true);
        participant.setPingedAt(LocalDateTime.now());
        participant = participantRepository.save(participant);

        // Send ping notification via LiveKit DataChannel
        // This will be handled by the frontend receiving the data packet

        return mapParticipantToDTO(participant);
    }

    @Transactional
    public void clearPing(UUID roomId, UUID studentId) {
        RoomParticipant participant = participantRepository
                .findByRoomIdAndStudentId(roomId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setIsPinged(false);
        participant.setPingedAt(null);
        participantRepository.save(participant);
    }

    public List<ParticipantDTO> getRoomParticipants(UUID roomId) {
        List<RoomParticipant> participants = participantRepository.findByRoomId(roomId);
        return participants.stream()
                .map(this::mapParticipantToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void startRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getStatus() != Room.RoomStatus.SCHEDULED) {
            throw new BadRequestException("Room is already LIVE or COMPLETED");
        }

        // Check if scheduled time has arrived (allow 15 minutes before scheduled time)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowedStartTime = room.getScheduledAt().minusMinutes(15);

        if (now.isBefore(allowedStartTime)) {
            throw new BadRequestException("Cannot start room before scheduled time. Scheduled at: " + room.getScheduledAt());
        }

        // Ensure LiveKit room name exists
        if (room.getLivekitRoomName() == null) {
            room.setLivekitRoomName("room-" + room.getId().toString());
        }

        // Try to create the LiveKit room. If this fails, abort starting.
        try {
            liveKitService.createLiveKitRoom(room.getLivekitRoomName(), room.getMaxStudents());
        } catch (Exception e) {
            throw new BadRequestException("Failed to create LiveKit room: " + e.getMessage());
        }

        room.setStatus(Room.RoomStatus.LIVE);
        roomRepository.save(room);
    }

    @Transactional
    public void endRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getStatus() != Room.RoomStatus.LIVE) {
            throw new BadRequestException("Room is not live");
        }

        room.setStatus(Room.RoomStatus.COMPLETED);
        roomRepository.save(room);

        // Trigger summary generation (handled by Python service)
    }

    /**
     * Validate if a user can join a room
     * Returns true if the user can join, throws BadRequestException otherwise
     */
    public boolean canJoinRoom(UUID roomId, UUID userId, User.UserRole userRole) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Check if scheduled time has arrived (allow 15 minutes before)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowedJoinTime = room.getScheduledAt().minusMinutes(15);

        if (now.isBefore(allowedJoinTime)) {
            throw new BadRequestException("Cannot join room before scheduled time. Scheduled at: " + room.getScheduledAt());
        }

        // Check if room is scheduled or live
        if (room.getStatus() != Room.RoomStatus.SCHEDULED && room.getStatus() != Room.RoomStatus.LIVE) {
            throw new BadRequestException("Room is not available. Status: " + room.getStatus());
        }

        // For professors: check if they are the assigned professor
        if (userRole == User.UserRole.PROFESSOR) {
            Professor professor = professorRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professor profile not found"));

            if (room.getProfessor() == null || !room.getProfessor().getId().equals(professor.getId())) {
                throw new BadRequestException("You are not assigned to this room");
            }
        }
        // For students: check if they are invited
        else if (userRole == User.UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

            Optional<RoomParticipant> participantOpt = participantRepository.findByRoomIdAndStudentId(roomId, student.getId());

            if (participantOpt.isEmpty() || !participantOpt.get().getInvited()) {
                throw new BadRequestException("You are not invited to this room");
            }
        }

        return true;
    }

    /**
     * Record that a student/professor joined the room
     */
    @Transactional
    public void recordJoin(UUID roomId, UUID userId, User.UserRole userRole) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (userRole == User.UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

            RoomParticipant participant = participantRepository.findByRoomIdAndStudentId(roomId, student.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

            if (participant.getJoinedAt() == null) {
                participant.setJoinedAt(LocalDateTime.now());
                participantRepository.save(participant);
            }
        }
        // For professors, we don't track join time in participants table
    }

    /**
     * Record that a user left the room.
     * If no active participants remain, set room status to COMPLETED.
     */
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId, User.UserRole userRole) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (userRole == User.UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

            Optional<RoomParticipant> participantOpt = participantRepository.findByRoomIdAndStudentId(roomId, student.getId());
            if (participantOpt.isPresent()) {
                RoomParticipant participant = participantOpt.get();
                if (participant.getLeftAt() == null) {
                    participant.setLeftAt(LocalDateTime.now());
                    participantRepository.save(participant);
                }
            }
        }
        // For professors: we don't track them in RoomParticipant,
        // but we still check if the room should be marked COMPLETED.

        // Check if any participants are still active in the room
        long activeCount = participantRepository.countByRoomIdAndJoinedAtIsNotNullAndLeftAtIsNull(roomId);
        if (activeCount == 0 && room.getStatus() == Room.RoomStatus.LIVE) {
            // If the professor is leaving AND no students remain, mark as COMPLETED
            room.setStatus(Room.RoomStatus.COMPLETED);
            roomRepository.save(room);
        }
    }

    public User getUserByEmail(String email) {
        // D'abord essayer de trouver par email
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }

        // Si non trouvé, essayer de trouver un étudiant par uniqueCode (numéro de téléphone)
        Optional<Student> studentByUniqueCode = studentRepository.findByUniqueCode(email);
        if (studentByUniqueCode.isPresent()) {
            return studentByUniqueCode.get().getUser();
        }

        throw new ResourceNotFoundException("User not found with email: " + email);
    }

    private RoomDTO mapToDTO(Room room) {
        List<RoomParticipant> participants = participantRepository.findByRoomId(room.getId());
        List<UUID> invitedStudents = participants.stream()
                .filter(RoomParticipant::getInvited)
                .map(p -> p.getStudent().getId())
                .collect(Collectors.toList());

        List<UUID> joinedStudents = participants.stream()
                .filter(p -> p.getJoinedAt() != null)
                .map(p -> p.getStudent().getId())
                .collect(Collectors.toList());

        return RoomDTO.builder()
                .id(room.getId())
                .name(room.getName())
                .language(room.getLanguage())
                .level(room.getLevel())
                .objective(room.getObjective())
                .scheduledAt(room.getScheduledAt())
                .duration(room.getDuration())
                .maxStudents(room.getMaxStudents())
                .status(room.getStatus())
                .animatorType(room.getAnimatorType())
                .professorId(room.getProfessor() != null ? room.getProfessor().getId() : null)
                .professorName(room.getProfessor() != null ? room.getProfessor().getUser().getName() : null)
                .livekitRoomName(room.getLivekitRoomName())
                .invitedStudents(invitedStudents)
                .joinedStudents(joinedStudents)
                .participantsCount(joinedStudents.size())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ParticipantDTO mapParticipantToDTO(RoomParticipant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .roomId(participant.getRoom().getId())
                .studentId(participant.getStudent().getId())
                .studentName(participant.getStudent().getUser().getName())
                .studentAvatar(participant.getStudent().getUser().getAvatar())
                .invited(participant.getInvited())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .isMuted(participant.getIsMuted())
                .isCameraOn(participant.getIsCameraOn())
                .isScreenSharing(participant.getIsScreenSharing())
                .handRaised(participant.getHandRaised())
                .isPinged(participant.getIsPinged())
                .pingedAt(participant.getPingedAt())
                .build();
    }
}
