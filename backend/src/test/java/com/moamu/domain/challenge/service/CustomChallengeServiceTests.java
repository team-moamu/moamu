package com.moamu.domain.challenge.service;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.moamu.domain.aitask.entity.AiTask;
import com.moamu.domain.aitask.enums.AiTaskType;
import com.moamu.domain.aitask.enums.AiTaskStatus;
import com.moamu.domain.aitask.repository.AiTaskRepository;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.artwork.repository.GeneratedArtworkMetadataStore;
import com.moamu.domain.challenge.dto.CustomChallengeCreateRequest;
import com.moamu.domain.challenge.dto.CustomChallengeCreateResponse;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.entity.ChallengeCondition;
import com.moamu.domain.challenge.repository.ChallengeConditionRepository;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.entity.GoalCategory;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.goal.entity.GoalTemplateCondition;
import com.moamu.domain.goal.enums.Status;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;
import com.moamu.domain.goal.repository.GoalAnalysisStore.CustomReceipt;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.repository.GoalCategoryRepository.CategoryNames;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.domain.goal.repository.GoalTemplateConditionRepository;
import com.moamu.domain.goal.repository.GoalTemplateRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomChallengeServiceTests {

    private static final String ANALYSIS_ID = "cc2e57ae-0f1b-49cf-9b2f-4c22f45f992b";
    private static final LocalDate START_DATE = LocalDate.of(2026, 11, 16);
    private static final GoalAnalysisSnapshot SNAPSHOT = new GoalAnalysisSnapshot(
        1L, "Daily walk", "산책하기", null, new CategoryPath("건강", "운동", "산책"),
        List.of("산책로가 보여야 해요.", "신발이 보여야 해요."), "길과 발끝을 촬영해 주세요.", "산책 흔적을 확인합니다."
    );

    @Mock private GoalAnalysisStore goalAnalysisStore;
    @Mock private GoalCategoryRepository goalCategoryRepository;
    @Mock private GoalTemplateRepository goalTemplateRepository;
    @Mock private GoalTemplateConditionRepository goalTemplateConditionRepository;
    @Mock private GoalTemplateArtworkRepository goalTemplateArtworkRepository;
    @Mock private AiTaskRepository aiTaskRepository;
    @Mock private ArtworkGenerationWorkStore artworkWorkStore;
    @Mock private GeneratedArtworkMetadataStore artworkMetadataStore;
    @Mock private ArtworkRepository artworkRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private ReusableArtworkService reusableArtworkService;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeConditionRepository challengeConditionRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private ChallengeService service;

    @ParameterizedTest
    @CsvSource({"7,3,3", "8,3,3", "14,4,4", "30,5,6", "90,9,10"})
    void createsPublicTemplateChallengeConditionsAndDailySchedules(int days, int rows, int cols) {
        stubConfirmation(SNAPSHOT);
        stubCreation();

        var response = service.createCustomChallenge(1L, request(days));

        var challengeCaptor = ArgumentCaptor.forClass(Challenge.class);
        verify(challengeRepository).save(challengeCaptor.capture());
        Challenge challenge = challengeCaptor.getValue();
        assertThat(response.challengeId()).isEqualTo(100L);
        assertThat(challenge.getUserId()).isEqualTo(1L);
        assertThat(challenge.getCateId()).isEqualTo(3L);
        assertThat(challenge.getGoalText()).isEqualTo(SNAPSHOT.normalizedGoal());
        assertThat(challenge.getChallengeTitle()).isEqualTo(SNAPSHOT.normalizedGoal());
        assertThat(challenge.getInputGoalText()).isEqualTo(SNAPSHOT.goal());
        assertThat(challenge.getPhotoGuide()).isEqualTo(SNAPSHOT.photoGuide());
        assertThat(challenge.getVerificationScope()).isEqualTo(SNAPSHOT.verificationScope());
        assertThat(challenge.getEndDate()).isEqualTo(START_DATE.plusDays(days - 1));
        assertThat(challenge.getGridRows()).isEqualTo(rows);
        assertThat(challenge.getGridCols()).isEqualTo(cols);

        var templateCaptor = ArgumentCaptor.forClass(GoalTemplate.class);
        verify(goalTemplateRepository).save(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getGoalName()).isEqualTo(SNAPSHOT.normalizedGoal());
        assertThat(templateCaptor.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        var categories = ArgumentCaptor.forClass(GoalCategory.class);
        verify(goalCategoryRepository, times(3)).saveAndFlush(categories.capture());
        assertThat(categories.getAllValues()).extracting(GoalCategory::getParentCateId).containsExactly(null, 1L, 2L);
        assertThat(categories.getAllValues()).allSatisfy(category -> {
            assertThat(category.getCateSource()).isEqualTo("AI_GENERATED");
            assertThat(category.getCateCode()).startsWith("AI_");
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChallengeCondition>> conditions = ArgumentCaptor.forClass(List.class);
        verify(challengeConditionRepository).saveAll(conditions.capture());
        assertThat(conditions.getValue()).extracting(ChallengeCondition::getCode).containsExactly("C1", "C2");
        assertThat(conditions.getValue()).extracting(ChallengeCondition::getDescription).isEqualTo(SNAPSHOT.conditions());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Schedule>> schedules = ArgumentCaptor.forClass(List.class);
        verify(scheduleRepository).saveAll(schedules.capture());
        assertThat(schedules.getValue()).hasSize(days);
        for (int index = 0; index < days; index++) {
            assertThat(schedules.getValue().get(index).getDayNo()).isEqualTo(index + 1);
            assertThat(schedules.getValue().get(index).getPieceIndex()).isEqualTo(index);
            assertThat(schedules.getValue().get(index).getScheduleDate()).isEqualTo(START_DATE.plusDays(index));
        }
        verify(transactionManager).commit(any());
        verify(goalAnalysisStore).saveConfirmation(ANALYSIS_ID, "lock-token", new GoalConfirmationSnapshot(1L, 100L));
        verify(goalAnalysisStore).finishConfirmation(ANALYSIS_ID, "lock-token");
    }

    @Test
    void returnsCachedConfirmationAfterAnalysisExpiry() {
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.of(new GoalConfirmationSnapshot(1L, 100L)));
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(completed(1L)));

        assertThat(service.createCustomChallenge(1L, request(7)).challengeId()).isEqualTo(100L);

        verify(goalAnalysisStore, never()).findByAnalysisId(anyString());
        verifyNoInteractions(transactionManager);
    }

    @Test
    void rejectsDifferentDatesOrDurationOnConfirmedAnalysis() {
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.of(new GoalConfirmationSnapshot(1L, 100L)));
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(completed(1L)));

        assertCode(() -> service.createCustomChallenge(1L, request(14)), "ANALYSIS_ALREADY_CONFIRMED");
        assertCode(() -> service.createCustomChallenge(1L,
            new CustomChallengeCreateRequest(ANALYSIS_ID, START_DATE.plusDays(1), START_DATE.plusDays(7))), "ANALYSIS_ALREADY_CONFIRMED");
        verify(goalAnalysisStore, never()).findByAnalysisId(anyString());
    }

    @Test
    void rejectsAnotherUsersConfirmedOrPendingAnalysis() {
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.of(new GoalConfirmationSnapshot(2L, 100L)));
        assertCode(() -> service.createCustomChallenge(1L, request(7)), "FORBIDDEN");

        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.empty());
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(SNAPSHOT));
        assertCode(() -> service.createCustomChallenge(2L, request(7)), "FORBIDDEN");
        verify(goalAnalysisStore, never()).acquireConfirmation(anyString(), any(), any(), any(), any());
    }

    @Test
    void expiredAnalysisReturns404() {
        assertCode(() -> service.createCustomChallenge(1L, request(7)), "ANALYSIS_NOT_FOUND");
        verifyNoInteractions(transactionManager);
    }

    @Test
    void parallelConfirmationReturns409() {
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(SNAPSHOT));

        assertCode(() -> service.createCustomChallenge(1L, request(7)), "ARTWORK_TASK_UNAVAILABLE");
        verifyNoInteractions(transactionManager);
    }

    @Test
    void staleExistingPathRollsBackAndReleasesLock() {
        GoalAnalysisSnapshot snapshot = new GoalAnalysisSnapshot(1L, SNAPSHOT.goal(), SNAPSHOT.normalizedGoal(), 3L,
            SNAPSHOT.categoryPath(), SNAPSHOT.conditions(), SNAPSHOT.photoGuide(), SNAPSHOT.verificationScope());
        stubConfirmation(snapshot);

        assertCode(() -> service.createCustomChallenge(1L, request(7)), "ANALYSIS_STALE");

        verify(transactionManager).rollback(any());
        verify(goalAnalysisStore, never()).finishConfirmation(ANALYSIS_ID, "lock-token");
        verifyNoInteractions(goalTemplateRepository, aiTaskRepository);
    }

    @Test
    void oldAnalysisWithoutNormalizedGoalRequiresNewAnalysis() {
        var snapshot = new GoalAnalysisSnapshot(1L, SNAPSHOT.goal(), null, null,
            SNAPSHOT.categoryPath(), SNAPSHOT.conditions(), SNAPSHOT.photoGuide(), SNAPSHOT.verificationScope());
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(snapshot));

        assertThatThrownBy(() -> service.createCustomChallenge(1L, request(7)))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode().getCode()).isEqualTo("ANALYSIS_STALE"));
        verify(goalAnalysisStore, never()).acquireConfirmation(anyString(), any(), any(), any(), any());
        verifyNoInteractions(challengeRepository, goalTemplateRepository);
    }

    @Test
    void restoresDeletedCategoriesAndReusesMatchingPublicTemplate() {
        stubConfirmation(SNAPSHOT);
        GoalCategory major = category(1L, null, "건강");
        GoalCategory middle = category(2L, 1L, "운동");
        GoalCategory minor = category(3L, 2L, "산책");
        ReflectionTestUtils.setField(major, "delYn", "Y");
        ReflectionTestUtils.setField(middle, "delYn", "Y");
        ReflectionTestUtils.setField(minor, "delYn", "Y");
        when(goalCategoryRepository.findForConfirmation(null, "건강")).thenReturn(Optional.of(major));
        when(goalCategoryRepository.findForConfirmation(1L, "운동")).thenReturn(Optional.of(middle));
        when(goalCategoryRepository.findForConfirmation(2L, "산책")).thenReturn(Optional.of(minor));
        when(goalCategoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, null, "건강")));
        when(goalCategoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, 1L, "운동")));
        when(goalCategoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, 2L, "산책")));
        GoalTemplate template = GoalTemplate.create(3L, SNAPSHOT.normalizedGoal(), SNAPSHOT.photoGuide(), SNAPSHOT.verificationScope());
        ReflectionTestUtils.setField(template, "goalTemplateId", 10L);
        when(goalTemplateRepository.findReusableTemplates(3L, SNAPSHOT.normalizedGoal(), SNAPSHOT.photoGuide(), SNAPSHOT.verificationScope()))
            .thenReturn(List.of(template));
        when(goalTemplateConditionRepository.findByGoalTemplateIdAndDelYn(10L, "N"))
            .thenReturn(List.of(GoalTemplateCondition.create(10L, "C2", SNAPSHOT.conditions().get(1)),
                GoalTemplateCondition.create(10L, "C1", SNAPSHOT.conditions().get(0))));
        stubArtworkAndChallenge();

        service.createCustomChallenge(1L, request(7));

        verify(goalCategoryRepository).restore(1L);
        verify(goalCategoryRepository).restore(2L);
        verify(goalCategoryRepository).restore(3L);
        verify(goalCategoryRepository, never()).saveAndFlush(any());
        verify(goalTemplateRepository, never()).save(any());
        verify(goalTemplateConditionRepository, never()).saveAll(any());
    }

    @Test
    void persistenceFailureRollsBackAndKeepsReceipt() {
        stubConfirmation(SNAPSHOT);
        when(goalCategoryRepository.findForConfirmation(null, "건강")).thenThrow(new IllegalStateException("DB unavailable"));

        assertThatThrownBy(() -> service.createCustomChallenge(1L, request(7))).isInstanceOf(IllegalStateException.class);

        verify(transactionManager).rollback(any());
        verify(goalAnalysisStore, never()).finishConfirmation(ANALYSIS_ID, "lock-token");
    }

    @Test
    void confirmationStorageFailureRollsBackCreation() {
        stubConfirmation(SNAPSHOT);
        stubCreation();
        doThrow(new IllegalStateException("Redis unavailable")).when(goalAnalysisStore)
            .saveConfirmation(ANALYSIS_ID, "lock-token", new GoalConfirmationSnapshot(1L, 100L));

        assertThatThrownBy(() -> service.createCustomChallenge(1L, request(7))).isInstanceOf(IllegalStateException.class);

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(goalAnalysisStore, never()).finishConfirmation(ANALYSIS_ID, "lock-token");
    }

    @Test
    void orphanedRedisResultDoesNotPreventRetryAfterDbRollback() {
        stubConfirmation(SNAPSHOT);
        stubCreation();
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.of(new GoalConfirmationSnapshot(1L, 99L)));

        assertThat(service.createCustomChallenge(1L, request(7)).challengeId()).isEqualTo(100L);

        verify(challengeRepository).findForConfirmation(99L);
        verify(goalAnalysisStore).saveConfirmation(ANALYSIS_ID, "lock-token", new GoalConfirmationSnapshot(1L, 100L));
    }

    @Test
    void waitsForPreviouslyRecordedChallengeBeforeCreatingAnother() {
        stubConfirmation(SNAPSHOT);
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID)).thenReturn(Optional.of(new GoalConfirmationSnapshot(1L, 100L)));
        when(challengeRepository.findForConfirmation(100L)).thenReturn(Optional.of(completed(1L)));

        assertThat(service.createCustomChallenge(1L, request(7)).challengeId()).isEqualTo(100L);

        verifyNoInteractions(goalCategoryRepository, goalTemplateRepository, aiTaskRepository);
        verify(challengeRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentCategoryCreationRetriesInANewTransaction(boolean lockConflict) {
        stubConfirmation(SNAPSHOT);
        stubCreation();
        when(goalCategoryRepository.findForConfirmation(null, "건강"))
            .thenThrow(lockConflict
                ? new CannotAcquireLockException("concurrent category insert")
                : new DataIntegrityViolationException("concurrent category insert"))
            .thenReturn(Optional.empty());

        service.createCustomChallenge(1L, request(7));

        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
        verify(transactionManager, times(2)).getTransaction(any());
    }

    @Test
    void validatesOwnerUuidAndDurationBeforeReadingStorage() {
        assertCode(() -> service.createCustomChallenge(null, request(7)), "UNAUTHORIZED");
        assertCode(() -> service.createCustomChallenge(1L, request(6)), "INVALID_REQUEST");
        assertCode(() -> service.createCustomChallenge(1L, request(91)), "INVALID_REQUEST");
        assertCode(() -> service.createCustomChallenge(1L,
            new CustomChallengeCreateRequest("bad-id", START_DATE, START_DATE.plusDays(6))), "INVALID_REQUEST");
        verifyNoInteractions(goalAnalysisStore, challengeRepository);
    }

    private void stubConfirmation(GoalAnalysisSnapshot snapshot) {
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(snapshot));
        when(reusableArtworkService.chooseArtwork(snapshot)).thenReturn(Optional.of(30L));
        when(goalAnalysisStore.acquireConfirmation(eq(ANALYSIS_ID), eq(snapshot), eq(START_DATE), any(), any())).thenReturn(Optional.of("lock-token"));
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
            return new SimpleTransactionStatus();
        });
    }

    @Test
    void commitsVerifiedArtworkChallengeAndTaskBeforePublishingSuccess() {
        var work = stubVerifiedWork(1);
        var task = AiTask.pendingArtworkGeneration(START_DATE.atStartOfDay());
        when(aiTaskRepository.findByAiTaskIdAndTaskType(91L, AiTaskType.GENERATE_ARTWORK))
                .thenReturn(Optional.of(task));
        stubCreation();
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            ReflectionTestUtils.setField(media, "mediaId", 20L);
            return media;
        });
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(invocation -> {
            Artwork artwork = invocation.getArgument(0);
            ReflectionTestUtils.setField(artwork, "artworkId", 30L);
            return artwork;
        });
        when(artworkWorkStore.beforeCommit(work, new GoalConfirmationSnapshot(1L, 100L))).thenReturn(true);

        service.confirmArtworkChallenge(work);

        assertThat(task.getStatus()).isEqualTo(AiTaskStatus.SUCCEEDED);
        assertThat(task.getArtworkId()).isEqualTo(30L);
        var order = inOrder(challengeRepository, artworkWorkStore, transactionManager);
        order.verify(challengeRepository).flush();
        order.verify(artworkWorkStore).beforeCommit(work, new GoalConfirmationSnapshot(1L, 100L));
        order.verify(transactionManager).commit(any());
        order.verify(artworkWorkStore).markSucceeded(work, 30L);
        verify(artworkMetadataStore).save(eq(30L), any());
    }

    @Test
    void rejectsAnOldVersionBeforeSavingAnything() {
        var work = stubVerifiedWork(2);
        var task = AiTask.pendingArtworkGeneration(START_DATE.atStartOfDay());
        when(aiTaskRepository.findByAiTaskIdAndTaskType(91L, AiTaskType.GENERATE_ARTWORK))
                .thenReturn(Optional.of(task));

        assertCode(() -> service.confirmArtworkChallenge(work), "ARTWORK_TASK_UNAVAILABLE");

        verify(transactionManager).rollback(any());
        verifyNoInteractions(mediaRepository, artworkRepository, challengeRepository);
        verify(artworkWorkStore, never()).markSucceeded(any(), any());
    }

    private ClaimedTask stubVerifiedWork(int version) {
        var work = new ClaimedTask(ANALYSIS_ID, "worker", 91L, version, Instant.now().plusSeconds(30),
                "{}", "{}", false, "{}", true, 91L, version, null);
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
                .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));
        when(artworkWorkStore.readAnalysis(work)).thenReturn(SNAPSHOT);
        var image = new ArtworkGenerationResult.Image("generated-artworks/91/" + version + "/result.webp",
                "image/webp", 100L, "0".repeat(64));
        var source = new ArtworkGenerationResult.Source("work-1", "Painting", "https://example.org/work",
                "PUBLIC_DOMAIN", "https://example.org/rights", Instant.EPOCH);
        when(artworkWorkStore.readResult(work)).thenReturn(new ArtworkGenerationResult(91L, version,
                ArtworkGenerationResult.Status.SUCCEEDED, null, null, image, source, true));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return work;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usesTheSameRepresentativeKeyForExistingAndNewCategoryPaths(boolean existingCategory) {
        var snapshot = new GoalAnalysisSnapshot(1L, SNAPSHOT.goal(), SNAPSHOT.normalizedGoal(),
                existingCategory ? 3L : null, SNAPSHOT.categoryPath(), SNAPSHOT.conditions(),
                SNAPSHOT.photoGuide(), SNAPSHOT.verificationScope());
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(snapshot));
        when(goalAnalysisStore.acquireConfirmation(eq(ANALYSIS_ID), eq(snapshot), eq(START_DATE), any(), any()))
            .thenReturn(Optional.of("lock-token"));
        when(goalCategoryRepository.findActiveMinorId("건강", "운동", "산책")).thenReturn(Optional.of(3L));
        CategoryNames names = mock(CategoryNames.class);
        when(names.getMajorName()).thenReturn("Health");
        when(names.getMiddleName()).thenReturn("Exercise");
        when(names.getMinorName()).thenReturn("Walking");
        when(goalCategoryRepository.findCategoryNames(3L)).thenReturn(Optional.of(names));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(aiTaskRepository.save(any(AiTask.class))).thenAnswer(invocation -> {
            AiTask task = invocation.getArgument(0);
            assertThat(task.getTaskType()).isEqualTo(AiTaskType.GENERATE_ARTWORK);
            ReflectionTestUtils.setField(task, "aiTaskId", 91L);
            return task;
        });
        when(goalAnalysisStore.registerArtworkTask(eq(ANALYSIS_ID), eq("lock-token"), eq("6:health8:exercise7:walking"),
                eq(91L), eq(1), any())).thenAnswer(invocation -> {
            Instant acceptedAt = invocation.getArgument(5);
            return new GoalAnalysisStore.PendingArtworkTask(91L, 1, 90L, 1, acceptedAt,
                    acceptedAt.plusSeconds(120));
        });

        var response = service.createCustomChallenge(1L, request(7));

        assertThat(response.taskId()).isEqualTo(91L);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.expiresAt()).isEqualTo(response.acceptedAt().plusSeconds(120));
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void returnsSameTaskForExistingReceiptWithoutReadingExpiredAnalysis() {
        var acceptedAt = Instant.now().minusSeconds(1);
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
            .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));
        when(goalAnalysisStore.findArtworkTask(ANALYSIS_ID))
            .thenReturn(Optional.of(new GoalAnalysisStore.PendingArtworkTask(91L, 1, 90L, 1,
                    acceptedAt, acceptedAt.plusSeconds(120))));

        var response = service.createCustomChallenge(1L, request(7));

        assertThat(response.taskId()).isEqualTo(91L);
        assertThat(response.challengeId()).isNull();
        verify(goalAnalysisStore, never()).findByAnalysisId(ANALYSIS_ID);
        verifyNoInteractions(aiTaskRepository);
    }

    @Test
    void sameReceiptReturnsConfirmedChallengeWithoutReadingAnalysis() {
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
            .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));
        when(goalAnalysisStore.findConfirmation(ANALYSIS_ID))
            .thenReturn(Optional.of(new GoalConfirmationSnapshot(1L, 100L)));
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(completed(1L)));

        assertThat(service.createCustomChallenge(1L, request(7)).challengeId()).isEqualTo(100L);
        verify(goalAnalysisStore, never()).findByAnalysisId(ANALYSIS_ID);
        verifyNoInteractions(transactionManager);
    }

    @Test
    void taskLookupHidesAnotherOwnersReceipt() {
        when(goalAnalysisStore.findTaskAnalysisId(91L)).thenReturn(Optional.of(ANALYSIS_ID));
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
                .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));

        assertCode(() -> service.getArtworkTask(2L, 91L), "ARTWORK_TASK_NOT_FOUND");
        assertCode(() -> service.retryArtworkTask(2L, 91L, 1), "ARTWORK_TASK_NOT_FOUND");
        verifyNoInteractions(aiTaskRepository);
    }

    @Test
    void retriesWithTheSameIdAndOnlyIncrementsTheVersionOnce() {
        stubOwnedTask();
        var task = stubRetryRow();
        when(artworkWorkStore.findFailure(ANALYSIS_ID, 91L, 1))
                .thenReturn(Optional.of(new ArtworkGenerationWorkStore.FailedTask("TEST_FAILURE", true)));
        when(goalAnalysisStore.readReceiptAnalysis(ANALYSIS_ID)).thenReturn(SNAPSHOT);
        when(goalAnalysisStore.publishRetry(eq(ANALYSIS_ID), eq(91L), eq(1), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(4));

        var response = service.retryArtworkTask(1L, 91L, 1);
        when(goalAnalysisStore.findRetryResult(ANALYSIS_ID, 1)).thenReturn(Optional.of(response));

        assertThat(service.retryArtworkTask(1L, 91L, 1)).isEqualTo(response);
        assertThat(response.taskId()).isEqualTo(91L);
        assertThat(response.taskVersion()).isEqualTo(2);
        assertThat(response.expiresAt()).isEqualTo(response.acceptedAt().plusSeconds(120));
        assertThat(task.getTaskVersion()).isEqualTo(2);
        verify(goalAnalysisStore).prepareRetry(eq(ANALYSIS_ID), eq(91L), eq(1), isNull(), any());
        verify(goalAnalysisStore).publishRetry(eq(ANALYSIS_ID), eq(91L), eq(1), any(), eq(response));
        verify(goalAnalysisStore, never()).findByAnalysisId(ANALYSIS_ID);
    }

    @Test
    void recoversTheStoredRetryPlanAfterTheDatabaseCommits() {
        stubOwnedTask();
        var task = stubRetryRow();
        task.restartArtworkGeneration();
        var plan = new GoalAnalysisStore.RetryPlan(Instant.now().minusSeconds(1), "walking", null);
        when(goalAnalysisStore.findRetryPlan(ANALYSIS_ID)).thenReturn(Optional.of(plan));
        when(goalAnalysisStore.publishRetry(eq(ANALYSIS_ID), eq(91L), eq(1), eq(plan), any()))
                .thenAnswer(invocation -> invocation.getArgument(4));

        var response = service.retryArtworkTask(1L, 91L, 1);

        assertThat(response.acceptedAt()).isEqualTo(plan.acceptedAt());
        assertThat(task.getTaskVersion()).isEqualTo(2);
        verifyNoInteractions(reusableArtworkService);
        verify(goalAnalysisStore, never()).prepareRetry(any(), anyLong(), anyInt(), any(), any());
    }

    @Test
    void rejectsRetryWhenTheFailureIsNotRetryable() {
        stubOwnedTask();
        stubRetryRow();
        when(artworkWorkStore.findFailure(ANALYSIS_ID, 91L, 1))
                .thenReturn(Optional.of(new ArtworkGenerationWorkStore.FailedTask("TEST_FAILURE", false)));

        assertCode(() -> service.retryArtworkTask(1L, 91L, 1), "ARTWORK_TASK_RETRY_CONFLICT");
        verifyNoInteractions(reusableArtworkService);
    }

    @Test
    void immediatelyConfirmsARetryWhenAnArtworkCanBeReused() {
        stubOwnedTask();
        var task = stubRetryRow();
        stubCreation();
        when(artworkWorkStore.findFailure(ANALYSIS_ID, 91L, 1))
                .thenReturn(Optional.of(new ArtworkGenerationWorkStore.FailedTask("TEST_FAILURE", true)));
        when(goalAnalysisStore.readReceiptAnalysis(ANALYSIS_ID)).thenReturn(SNAPSHOT);
        when(reusableArtworkService.chooseArtwork(SNAPSHOT)).thenReturn(Optional.of(30L));
        when(goalAnalysisStore.publishRetry(eq(ANALYSIS_ID), eq(91L), eq(1), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(4));

        assertThat(service.retryArtworkTask(1L, 91L, 1)).isEqualTo(new CustomChallengeCreateResponse(100L));
        assertThat(task.getTaskVersion()).isEqualTo(2);
        assertThat(task.getStatus()).isEqualTo(AiTaskStatus.SUCCEEDED);
        var order = inOrder(goalAnalysisStore, transactionManager);
        order.verify(goalAnalysisStore).saveRetryChallenge(eq(ANALYSIS_ID), eq(1), any(),
                eq(new GoalConfirmationSnapshot(1L, 100L)));
        order.verify(transactionManager).commit(any());
        order.verify(goalAnalysisStore).publishRetry(eq(ANALYSIS_ID), eq(91L), eq(1), any(),
                eq(new CustomChallengeCreateResponse(100L)));
    }

    private void stubOwnedTask() {
        when(goalAnalysisStore.findTaskAnalysisId(91L)).thenReturn(Optional.of(ANALYSIS_ID));
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
                .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));
        var accepted = Instant.now().minusSeconds(1);
        when(goalAnalysisStore.findArtworkTask(ANALYSIS_ID)).thenReturn(Optional.of(
                new GoalAnalysisStore.PendingArtworkTask(91L, 1, 91L, 1, accepted, accepted.plusSeconds(120))));
    }

    private AiTask stubRetryRow() {
        var task = AiTask.pendingArtworkGeneration(START_DATE.atStartOfDay());
        ReflectionTestUtils.setField(task, "aiTaskId", 91L);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(aiTaskRepository.findByAiTaskIdAndTaskType(91L, AiTaskType.GENERATE_ARTWORK))
                .thenReturn(Optional.of(task));
        return task;
    }

    @Test
    void receiptGuardsItsOwnerAndScheduleBeforeConfirmation() {
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID))
            .thenReturn(Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));

        assertCode(() -> service.createCustomChallenge(2L, request(7)), "FORBIDDEN");
        assertCode(() -> service.createCustomChallenge(1L, request(8)), "ANALYSIS_ALREADY_CONFIRMED");
        assertCode(() -> service.createCustomChallenge(1L, request(7)), "ARTWORK_TASK_UNAVAILABLE");
        verify(goalAnalysisStore, never()).findByAnalysisId(ANALYSIS_ID);
        verifyNoInteractions(transactionManager);
    }

    @Test
    void simultaneousSubmissionUsesTheWinningReceipt() {
        when(goalAnalysisStore.findCustomReceipt(ANALYSIS_ID)).thenReturn(Optional.empty(),
                Optional.of(new CustomReceipt(1L, START_DATE, START_DATE.plusDays(6))));
        when(goalAnalysisStore.findByAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(SNAPSHOT));

        assertCode(() -> service.createCustomChallenge(1L, request(8)), "ANALYSIS_ALREADY_CONFIRMED");
        verifyNoInteractions(transactionManager);
    }

    @Test
    void doesNotRetryWhenCommitOutcomeIsUnknown() {
        stubConfirmation(SNAPSHOT);
        stubCreation();
        var failure = new org.springframework.dao.TransientDataAccessResourceException("Commit outcome unknown");
        doThrow(failure).when(transactionManager).commit(any());

        assertCode(() -> service.createCustomChallenge(1L, request(7)), "ARTWORK_TASK_UNAVAILABLE");
        verify(transactionManager).commit(any());
        verify(goalAnalysisStore, never()).finishConfirmation(ANALYSIS_ID, "lock-token");
    }

    private void stubCreation() {
        AtomicLong nextId = new AtomicLong();
        when(goalCategoryRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            GoalCategory category = invocation.getArgument(0);
            ReflectionTestUtils.setField(category, "cateId", nextId.incrementAndGet());
            return category;
        });
        when(goalTemplateRepository.save(any())).thenAnswer(invocation -> {
            GoalTemplate template = invocation.getArgument(0);
            ReflectionTestUtils.setField(template, "goalTemplateId", 10L);
            return template;
        });
        stubArtworkAndChallenge();
    }

    private void stubArtworkAndChallenge() {
        when(challengeRepository.save(any())).thenAnswer(invocation -> {
            Challenge challenge = invocation.getArgument(0);
            ReflectionTestUtils.setField(challenge, "challengeId", 100L);
            return challenge;
        });
    }

    private GoalCategory category(Long id, Long parentId, String name) {
        GoalCategory category = GoalCategory.create(parentId, name, "TEST_" + id);
        ReflectionTestUtils.setField(category, "cateId", id);
        return category;
    }

    private Challenge completed(Long owner) {
        Challenge challenge = Challenge.create(owner, 3L, 10L, 30L, SNAPSHOT.goal(), SNAPSHOT.goal(), SNAPSHOT.goal(),
            SNAPSHOT.photoGuide(), 7, START_DATE, START_DATE.plusDays(6), SNAPSHOT.verificationScope(), 3, 3);
        ReflectionTestUtils.setField(challenge, "challengeId", 100L);
        return challenge;
    }

    private CustomChallengeCreateRequest request(int days) {
        return new CustomChallengeCreateRequest(ANALYSIS_ID, START_DATE, START_DATE.plusDays(days - 1));
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode().getCode()).isEqualTo(code));
    }
}
