package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HybridSimplePostBoxRepositoryTest {

    private static final boolean WITH_DEEP_MIGRATION = true;
    private static final boolean DELETE_CTHREAD_ENABLED = true;

    @Mock
    private RiakSimplePostBoxRepository riakRepository;

    @Mock
    private CassandraSimplePostBoxRepository cassandraRepository;

    @Mock
    private HybridMigrationClusterState migrationState;

    private HybridSimplePostBoxRepository normalRepository;
    private HybridSimplePostBoxRepository deepMigrationRepository;

    @Before
    public void setup() {
        normalRepository = new HybridSimplePostBoxRepository(riakRepository, cassandraRepository, migrationState,
                WITH_DEEP_MIGRATION, DELETE_CTHREAD_ENABLED);
        deepMigrationRepository = new HybridSimplePostBoxRepository(riakRepository, cassandraRepository, migrationState,
                WITH_DEEP_MIGRATION, DELETE_CTHREAD_ENABLED);
    }

    @Test
    public void nonDeepMigrationWritesOnlyFullPostbox() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        AbstractConversationThread c2 = createConversationThread(DateTime.now().minusMinutes(10), "c2");
        PostBox riakPostbox = new PostBox(email, Optional.of(0L), newArrayList(c1, c2), 10);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), Collections.emptyList(), 10));
        when(riakRepository.byId(email)).thenReturn(riakPostbox);
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(true);

        normalRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verify(cassandraRepository).write(riakPostbox);
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void nonDeepMigrationDoesntWriteIfCassandraAlreadyHasPostbox() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        AbstractConversationThread c2 = createConversationThread(DateTime.now().minusMinutes(10), "c2");
        AbstractConversationThread c3 = createConversationThread(DateTime.now().minusMinutes(10), "c3");
        List cassConvThreads = newArrayList(c1, c2);
        List riakConvThreads = newArrayList(c2, c3);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), cassConvThreads, 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(true);

        deepMigrationRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void nonDeepMigrationDoesntWriteIfPostboxCannotBeClaimed() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        List riakConvThreads = newArrayList(c1);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), Collections.emptyList(), 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(false);

        normalRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void deepMigrationWritesUpdatedAndNewConversationThreadsToCassandra() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        AbstractConversationThread c2 = createConversationThread(DateTime.now().minusMinutes(10), "c2");
        AbstractConversationThread c2Updated = createConversationThread(DateTime.now().minusMinutes(1), "c2");
        AbstractConversationThread c3 = createConversationThread(DateTime.now(), "c3");
        List cassConvThreads = newArrayList(c1, c2);
        List riakConvThreads = newArrayList(c1, c2Updated, c3);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), cassConvThreads, 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(true);

        deepMigrationRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verify(cassandraRepository).writeThread(email, c2Updated);
        verify(cassandraRepository).writeThread(email, c3);
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void deepMigrationDeletesDeletedConversationThreadsFromCassandra() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        AbstractConversationThread c2 = createConversationThread(DateTime.now().minusMinutes(10), "c2");
        List cassConvThreads = newArrayList(c1, c2);
        List riakConvThreads = newArrayList(c1);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), cassConvThreads, 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(true);

        deepMigrationRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verify(cassandraRepository).deleteConversationThreads(email, Arrays.asList("c2"));
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void deepMigrationDoNotRemoveConversationThreadsFromCassandra() throws Exception {

        deepMigrationRepository = new HybridSimplePostBoxRepository(riakRepository, cassandraRepository, migrationState,
                WITH_DEEP_MIGRATION, false);

        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        AbstractConversationThread c2 = createConversationThread(DateTime.now().minusMinutes(10), "c2");
        List cassConvThreads = newArrayList(c1, c2);
        List riakConvThreads = newArrayList(c1);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), cassConvThreads, 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(true);

        deepMigrationRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verifyNoMoreInteractions(cassandraRepository);
    }

    @Test
    public void deepMigrationDoesntWriteIfPostboxCannotBeClaimed() throws Exception {
        String email = "test@example.com";
        AbstractConversationThread c1 = createConversationThread(DateTime.now().minusHours(1), "c1");
        List riakConvThreads = newArrayList(c1);

        when(cassandraRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), Collections.emptyList(), 10));
        when(riakRepository.byId(email)).thenReturn(new PostBox(email, Optional.of(0L), riakConvThreads, 10));
        when(migrationState.tryClaim(PostBox.class, email)).thenReturn(false);

        deepMigrationRepository.byId(email);

        verify(cassandraRepository).byId(email);
        verifyNoMoreInteractions(cassandraRepository);
    }

    private AbstractConversationThread createConversationThread(DateTime date, String conversationId) {
        return new PostBoxTest.ConversationThread("123", conversationId, date, date, date, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

}
