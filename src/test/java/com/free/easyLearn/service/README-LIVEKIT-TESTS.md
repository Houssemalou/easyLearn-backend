# Tests LiveKit Service

## Description

Ce fichier contient les tests unitaires pour le service LiveKit (`LiveKitService.java`). Ces tests vérifient le bon fonctionnement de la création de rooms LiveKit, la génération de tokens d'accès, et les permissions associées.

## Tests implémentés

### 1. Tests de création de room LiveKit

#### `testCreateLiveKitRoom_Success`
- **Objectif**: Vérifier qu'une room LiveKit peut être créée avec succès
- **Comportement**: Appelle `roomServiceClient.createRoom()` avec les bons paramètres
- **Assertion**: Vérifie que la méthode est appelée exactement une fois

#### `testCreateLiveKitRoom_RoomAlreadyExists`
- **Objectif**: Vérifier que l'erreur "room existe déjà" est gérée correctement
- **Comportement**: Simule une exception lors de la création
- **Assertion**: Vérifie que l'exception n'est pas propagée (gestion gracieuse)

### 2. Tests de génération de token

#### `testGenerateToken_CreatesRoom_ForProfessor`
- **Objectif**: Vérifier qu'un professeur peut générer un token et créer une room
- **Comportement**: 
  - Le professeur génère un token pour une room LIVE
  - La room LiveKit est automatiquement créée
  - Un token valide est retourné
- **Assertions**:
  - Token généré avec succès
  - Room LiveKit créée
  - Token sauvegardé en base de données

#### `testGenerateToken_DoesNotCreateRoom_ForStudent`
- **Objectif**: Vérifier qu'un étudiant peut rejoindre mais ne crée pas de room
- **Comportement**: Un étudiant génère un token pour une room déjà créée
- **Assertions**:
  - Token généré avec succès
  - Aucune tentative de création de room
  - Token sauvegardé en base de données

#### `testGenerateToken_CreatesLivekitRoomName`
- **Objectif**: Vérifier la génération automatique du nom LiveKit
- **Comportement**: Si `livekitRoomName` est null, il est généré automatiquement
- **Format**: `room-{UUID}`
- **Assertion**: Le nom est bien généré et sauvegardé

### 3. Tests de gestion des erreurs

#### `testGenerateToken_RoomNotFound`
- **Objectif**: Vérifier le comportement quand la room n'existe pas
- **Exception attendue**: `ResourceNotFoundException`

#### `testGenerateToken_UserNotFound`
- **Objectif**: Vérifier le comportement quand l'utilisateur n'existe pas
- **Exception attendue**: `ResourceNotFoundException`

#### `testGenerateToken_RoomNotLive`
- **Objectif**: Vérifier qu'on ne peut pas rejoindre une room non LIVE
- **Exception attendue**: `BadRequestException` avec message explicite

### 4. Tests de permissions

#### `testGenerateToken_AdminCanCreateRoom`
- **Objectif**: Vérifier qu'un administrateur peut créer une room
- **Comportement**: Similaire au professeur, l'admin peut créer des rooms
- **Assertion**: Room créée avec succès

### 5. Tests de suppression de room

#### `testDeleteLiveKitRoom_Success`
- **Objectif**: Vérifier la suppression d'une room LiveKit
- **Assertion**: `roomServiceClient.deleteRoom()` est appelé

#### `testDeleteLiveKitRoom_WithError`
- **Objectif**: Vérifier la gestion d'erreur lors de la suppression
- **Comportement**: L'erreur est gérée gracieusement (pas d'exception propagée)

## Structure des données de test

### Room de test
```java
Room testRoom = Room.builder()
    .id(roomId)
    .name("Test Room")
    .language("English")
    .level(Student.LanguageLevel.B1)
    .objective("Test objective")
    .scheduledAt(LocalDateTime.now().plusHours(1))
    .duration(60)
    .maxStudents(30)
    .status(Room.RoomStatus.LIVE)  // Important: doit être LIVE
    .animatorType(Room.AnimatorType.PROFESSOR)
    .professor(professorEntity)
    .build();
```

### Utilisateurs de test
- **Professeur**: `User.UserRole.PROFESSOR`
- **Étudiant**: `User.UserRole.STUDENT`
- **Admin**: `User.UserRole.ADMIN`

## Configuration des tests

### Mocks utilisés
- `LiveKitConfig`: Configuration LiveKit (API key, secret, URL)
- `RoomServiceClient`: Client LiveKit pour créer/supprimer les rooms
- `LiveKitTokenRepository`: Repository pour sauvegarder les tokens
- `RoomRepository`: Repository pour les rooms
- `UserRepository`: Repository pour les utilisateurs

### Configuration strictness
```java
@MockitoSettings(strictness = Strictness.LENIENT)
```
Permet d'éviter les erreurs de "unnecessary stubbing" quand certains mocks ne sont pas utilisés dans tous les tests.

## Exécution des tests

### Via Maven
```bash
# Exécuter tous les tests LiveKit
./mvnw test -Dtest=LiveKitServiceTest

# Exécuter un test spécifique
./mvnw test -Dtest=LiveKitServiceTest#testCreateLiveKitRoom_Success
```

### Via IDE
- Clic droit sur la classe > Run 'LiveKitServiceTest'
- Ou exécuter un test individuel

## Résultats attendus

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

Tous les tests doivent passer avec succès.

## Points importants

### Permissions de création de room
Seuls peuvent créer une room LiveKit:
- Les **professeurs** (role PROFESSOR)
- Les **administrateurs** (role ADMIN)
- Le **professeur assigné** à la room

Les étudiants peuvent uniquement rejoindre une room déjà créée.

### Gestion des erreurs
Le service gère gracieusement les erreurs LiveKit:
- Si une room existe déjà, pas d'exception levée
- Si la suppression échoue, l'erreur est loggée mais pas propagée

### Token expiration
- Durée par défaut: 3600 secondes (1 heure)
- Configurable via `livekit.token-expiration`
- Les tokens expirés sont nettoyés automatiquement

## Améliorations futures

1. Tests d'intégration avec un vrai serveur LiveKit
2. Tests de charge pour vérifier la scalabilité
3. Tests de webhooks LiveKit
4. Tests de gestion des participants
5. Tests de recording

## Dépendances

```xml
<!-- Test dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- LiveKit SDK -->
<dependency>
    <groupId>io.livekit</groupId>
    <artifactId>livekit-server</artifactId>
    <version>0.8.5</version>
</dependency>
```

## Auteur

Tests créés pour le backend easyLearn du projet Lingua Hub.
