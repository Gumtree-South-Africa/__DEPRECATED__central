package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.*;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultBlockUserRepository implements BlockUserRepository, CassandraRepository {
    private final Session session;
    private final Map<StatementsBase, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    public DefaultBlockUserRepository(
            Session session,
            ConsistencyLevel readConsistency,
            ConsistencyLevel writeConsistency
    ) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
    }

    @Override
    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    @Override
    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    @Override
    public Map<StatementsBase, PreparedStatement> getPreparedStatements() {
        return preparedStatements;
    }

    @Override
    public void blockUser(String reporterUserId, String userIdToBlock) {
        session.execute(Statements.BLOCK_USER.bind(this, reporterUserId, userIdToBlock, DateTime.now().toDate()));
    }

    @Override
    public void unblockUser(String reporterUserId, String userIdToUnblock) {
        session.execute(Statements.UNBLOCK_USER.bind(this, reporterUserId, userIdToUnblock));
    }

    @Override
    public Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId2) {
        List<Supplier<ResultSet>> results = newArrayList(
                () -> session.execute(Statements.SELECT_BLOCKED_USER.bind(this, userId1, userId2)),
                () -> session.execute(Statements.SELECT_BLOCKED_USER.bind(this, userId2, userId1))
        );

        for (Supplier<ResultSet> result : results) {
            Row row = result.get().one();
            if (row != null) {
                return Optional.of(
                        new BlockedUserInfo(row.getString("blockerid"), row.getString("blockeeid"), new DateTime(row.getDate("blockdate")))
                );
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean areUsersBlocked(String userId1, String userId2) {
        return getBlockedUserInfo(userId1, userId2).isPresent();
    }

    static class Statements extends StatementsBase {
        static Statements BLOCK_USER = new Statements("INSERT INTO core_blocked_users (blockerid, blockeeid, blockdate) VALUES (?, ?, ?)", true);
        static Statements UNBLOCK_USER = new Statements("DELETE FROM core_blocked_users WHERE blockerid = ? AND blockeeid = ?", true);
        static Statements SELECT_BLOCKED_USER = new Statements("SELECT blockerid, blockeeid, blockdate FROM core_blocked_users WHERE blockerid = ? AND blockeeid = ?", false);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }

}
