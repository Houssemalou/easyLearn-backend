package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column
    private Boolean invited = false;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_muted")
    private Boolean isMuted = false;

    @Column(name = "is_camera_on")
    private Boolean isCameraOn = true;

    @Column(name = "is_screen_sharing")
    private Boolean isScreenSharing = false;

    @Column(name = "hand_raised")
    private Boolean handRaised = false;

    @Column(name = "is_pinged")
    private Boolean isPinged = false;

    @Column(name = "pinged_at")
    private LocalDateTime pingedAt;
}