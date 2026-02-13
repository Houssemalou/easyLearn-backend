package com.free.easyLearn.service;

import com.free.easyLearn.config.LiveKitConfig;
import com.free.easyLearn.dto.livekit.LiveKitTokenResponse;
import com.free.easyLearn.entity.*;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.LiveKitTokenRepository;
import com.free.easyLearn.repository.RoomRepository;
import com.free.easyLearn.repository.UserRepository;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitaire pour LiveKitService
 * Teste principalement la création de rooms LiveKit
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiveKitServiceTest {

    @Mock
    private LiveKitConfig livekitConfig;

    @Mock
    private RoomServiceClient roomServiceClient;

    @Mock
    private LiveKitTokenRepository liveKitTokenRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LiveKitService liveKitService;

    private Room testRoom;
    private User testProfessor;
    private User testStudent;
    private Professor professorEntity;
    private UUID roomId;
    private UUID professorId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        // Configuration des IDs
        roomId = UUID.randomUUID();
        professorId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        // Configuration du professeur
        testProfessor = User.builder()
                .id(professorId)
                .name("Prof Test")
                .email("prof@test.com")
                .role(User.UserRole.PROFESSOR)
                .build();

        professorEntity = Professor.builder()
                .id(UUID.randomUUID())
                .user(testProfessor)
                .build();

        // Configuration de l'étudiant
        testStudent = User.builder()
                .id(studentId)
                .name("Student Test")
                .email("student@test.com")
                .role(User.UserRole.STUDENT)
                .build();

        // Configuration de la room
        testRoom = Room.builder()
                .id(roomId)
                .name("Test Room")
                .language("English")
                .level(Student.LanguageLevel.B1)
                .objective("Test objective")
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .duration(60)
                .maxStudents(30)
                .status(Room.RoomStatus.LIVE)
                .animatorType(Room.AnimatorType.PROFESSOR)
                .professor(professorEntity)
                .build();

        // Configuration du service LiveKit
        when(livekitConfig.getApiKey()).thenReturn("test-api-key");
        when(livekitConfig.getApiSecret()).thenReturn("test-api-secret");
        when(livekitConfig.getLivekitUrl()).thenReturn("ws://localhost:7880");
        
        // Définir le tokenExpiration via reflection
        ReflectionTestUtils.setField(liveKitService, "tokenExpiration", 3600L);
    }

    /**
     * Test de création d'une room LiveKit avec succès par un professeur
     */
    @Test
    void testCreateLiveKitRoom_Success() throws Exception {
        // Given
        String roomName = "room-" + roomId.toString();
        int maxParticipants = 30;
        
        // Mock de la réponse LiveKit
        LivekitRoom.CreateRoomRequest mockRequest = LivekitRoom.CreateRoomRequest.newBuilder()
                .setName(roomName)
                .setMaxParticipants(maxParticipants)
                .build();

        // When
        liveKitService.createLiveKitRoom(roomName, maxParticipants);

        // Then
        verify(roomServiceClient, times(1)).createRoom(eq(roomName), eq(maxParticipants), eq(300));
    }

    /**
     * Test de création d'une room LiveKit qui échoue (room existe déjà)
     */
    @Test
    void testCreateLiveKitRoom_RoomAlreadyExists() throws Exception {
        // Given
        String roomName = "room-" + roomId.toString();
        int maxParticipants = 30;
        
        // Simuler une erreur lors de la création (room existe déjà)
        doThrow(new RuntimeException("Room already exists"))
                .when(roomServiceClient).createRoom(anyString(), anyInt(), anyInt());

        // When & Then - La méthode ne doit pas lever d'exception
        assertDoesNotThrow(() -> liveKitService.createLiveKitRoom(roomName, maxParticipants));
        
        // Vérifier que l'appel a bien été fait
        verify(roomServiceClient, times(1)).createRoom(eq(roomName), eq(maxParticipants), eq(300));
    }

    /**
     * Test de génération de token et création automatique de room par un professeur
     */
    @Test
    void testGenerateToken_CreatesRoom_ForProfessor() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(liveKitTokenRepository.save(any(LiveKitToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        LiveKitTokenResponse response = liveKitService.generateToken(roomId, professorId);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("room-" + roomId.toString(), response.getRoomName());
        assertEquals("Prof Test", response.getIdentity());
        assertEquals("ws://localhost:7880", response.getServerUrl());
        
        // Vérifier que la room LiveKit a été créée
        verify(roomServiceClient, times(1)).createRoom(anyString(), eq(30), eq(300));
        
        // Vérifier que le token a été sauvegardé
        ArgumentCaptor<LiveKitToken> tokenCaptor = ArgumentCaptor.forClass(LiveKitToken.class);
        verify(liveKitTokenRepository, times(1)).save(tokenCaptor.capture());
        
        LiveKitToken savedToken = tokenCaptor.getValue();
        assertEquals(testProfessor, savedToken.getUser());
        assertEquals(testRoom, savedToken.getRoom());
        assertEquals("Prof Test", savedToken.getIdentity());
    }

    /**
     * Test de génération de token pour un étudiant (ne crée pas la room)
     */
    @Test
    void testGenerateToken_DoesNotCreateRoom_ForStudent() {
        // Given
        testRoom.setLivekitRoomName("room-" + roomId.toString()); // Room déjà créée
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(liveKitTokenRepository.save(any(LiveKitToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        LiveKitTokenResponse response = liveKitService.generateToken(roomId, studentId);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("room-" + roomId.toString(), response.getRoomName());
        
        // Vérifier que la room LiveKit n'a PAS été créée
        verify(roomServiceClient, never()).createRoom(anyString(), anyInt(), anyInt());
        
        // Vérifier que le token a été sauvegardé
        verify(liveKitTokenRepository, times(1)).save(any(LiveKitToken.class));
    }

    /**
     * Test de génération de token avec room non trouvée
     */
    @Test
    void testGenerateToken_RoomNotFound() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> liveKitService.generateToken(roomId, professorId));
        
        verify(roomServiceClient, never()).createRoom(anyString(), anyInt(), anyInt());
    }

    /**
     * Test de génération de token avec utilisateur non trouvé
     */
    @Test
    void testGenerateToken_UserNotFound() {
        // Given
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(professorId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> liveKitService.generateToken(roomId, professorId));
        
        verify(roomServiceClient, never()).createRoom(anyString(), anyInt(), anyInt());
    }

    /**
     * Test de génération de token avec room non LIVE
     */
    @Test
    void testGenerateToken_RoomNotLive() {
        // Given
        testRoom.setStatus(Room.RoomStatus.SCHEDULED);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> liveKitService.generateToken(roomId, professorId));
        
        assertTrue(exception.getMessage().contains("Room is not live yet"));
        verify(roomServiceClient, never()).createRoom(anyString(), anyInt(), anyInt());
    }

    /**
     * Test de suppression d'une room LiveKit avec succès
     */
    @Test
    void testDeleteLiveKitRoom_Success() throws Exception {
        // Given
        String roomName = "room-" + roomId.toString();

        // When
        liveKitService.deleteLiveKitRoom(roomName);

        // Then
        verify(roomServiceClient, times(1)).deleteRoom(eq(roomName));
    }

    /**
     * Test de suppression d'une room LiveKit avec erreur
     */
    @Test
    void testDeleteLiveKitRoom_WithError() throws Exception {
        // Given
        String roomName = "room-" + roomId.toString();
        doThrow(new RuntimeException("Room not found"))
                .when(roomServiceClient).deleteRoom(anyString());

        // When & Then - La méthode ne doit pas lever d'exception
        assertDoesNotThrow(() -> liveKitService.deleteLiveKitRoom(roomName));
        
        verify(roomServiceClient, times(1)).deleteRoom(eq(roomName));
    }

    /**
     * Test de création automatique du nom de room LiveKit
     */
    @Test
    void testGenerateToken_CreatesLivekitRoomName() {
        // Given
        testRoom.setLivekitRoomName(null); // Pas de nom LiveKit
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArguments()[0]);
        when(liveKitTokenRepository.save(any(LiveKitToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        LiveKitTokenResponse response = liveKitService.generateToken(roomId, professorId);

        // Then
        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(1)).save(roomCaptor.capture());
        
        Room savedRoom = roomCaptor.getValue();
        assertNotNull(savedRoom.getLivekitRoomName());
        assertEquals("room-" + roomId.toString(), savedRoom.getLivekitRoomName());
        assertEquals("room-" + roomId.toString(), response.getRoomName());
    }

    /**
     * Test de vérification des permissions - Admin peut créer une room
     */
    @Test
    void testGenerateToken_AdminCanCreateRoom() {
        // Given
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin Test")
                .email("admin@test.com")
                .role(User.UserRole.ADMIN)
                .build();
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(liveKitTokenRepository.save(any(LiveKitToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        LiveKitTokenResponse response = liveKitService.generateToken(roomId, adminUser.getId());

        // Then
        assertNotNull(response);
        verify(roomServiceClient, times(1)).createRoom(anyString(), eq(30), eq(300));
    }
}
