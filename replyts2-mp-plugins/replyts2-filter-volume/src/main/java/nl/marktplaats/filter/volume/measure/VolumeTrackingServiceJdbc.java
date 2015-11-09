package nl.marktplaats.filter.volume.measure;

import nl.marktplaats.filter.volume.VolumeFilterConfiguration;
import org.springframework.stereotype.Component;

/**
 * JDBC Volume Tracking Service. This service has an optimistic approach on storing volume info. The table is:
 * <p/>
 * <pre>
 * +------------+--------------+------+-----+---------+-------+
 * | Field      | Type         | Null | Key | Default | Extra |
 * +------------+--------------+------+-----+---------+-------+
 * | userId     | varchar(120) | NO   | PRI | NULL    |       |
 * | received   | bigint(20)   | NO   | PRI | NULL    |       |
 * | platformId | varchar(40)  | NO   | PRI | NULL    |       |
 * +------------+--------------+------+-----+---------+-------+
 * </pre>
 * <p/>
 * All three fields form a primary key together, where the received col is a timestamp in milliseconds. Even tough there
 * is a <em>potential</em> risk to lose some tracked mails, this one can very well be ignored. The chance that a
 * significant amount of mails from the same sender will drop in and become processed in the same millisecond is
 * extremely low.
 *
 * @author huttar
 */
@Component
public class VolumeTrackingServiceJdbc implements VolumeTrackingService {
    @Override
    public void record(String userId) {

    }

    @Override
    public boolean violates(String user, VolumeFilterConfiguration.VolumeRule rule) {
        return false;
    }

    @Override
    public void cleanup() {

    }
//
//    private static final int DELETE_BULK_SIZE = 500;
//    private static final int MAX_DELETE_ITERATIONS = 10;
//    @Autowired
//    private DataSource dataSource;
//
//    private final static String RECORD = "insert into volumefilter(userId, received, platformId) values(?, ?, ?)";
//
//    private final static String COUNT = "select count(*) from volumefilter where userId=? and platformId=? and received > ?";
//
//    private final static String CLEANUP_SELECT = "select userId, received from volumefilter where received < ? and platformId = ? limit ?";
//
//    private final static String DELETE = "delete from volumefilter where userId=? and platformId=? and received=?";
//
//    private JdbcTemplate jdbcTemplate;
//
//    private AtomicLong lastWrittenTimeStamp = new AtomicLong(0L);
//
//    private static final Logger LOG = LoggerFactory.getLogger(VolumeTrackingServiceJdbc.class);
//
//    @PostConstruct
//    private void createTemplate() {
//        jdbcTemplate = new JdbcTemplate(dataSource);
//    }
//
//    @Override
//    public void record(String userId, String platformId) {
//        long now = System.currentTimeMillis();
//        long lwts;
//        do {
//            lwts = lastWrittenTimeStamp.get();
//            if (now <= lwts) {
//                now = lwts + 1;
//            }
//        } while (!lastWrittenTimeStamp.compareAndSet(lwts, now));
//
//        record(userId, now, platformId);
//    }
//
//    /**
//     * Creates a record of a user, with all entires being modificable. Not Exposed through interface
//     *
//     * @param userId     user id
//     * @param timestamp  timestamp of mail
//     * @param platformId platform that was affected
//     */
//    public void record(String userId, long timestamp, String platformId) {
//        jdbcTemplate.update(RECORD, userId, timestamp, platformId);
//    }
//
//    @Override
//    public boolean violates(String user, String platform,
//                            VolumeFilterConfiguration.VolumeRule rule) {
//        long after = System.currentTimeMillis()
//                - rule.getTimeUnit().toMillis(rule.getTimeSpan());
//
//        int sentActually = countMails(user, platform, after);
//
//        return rule.getMaxCount() < sentActually;
//
//    }
//
//    public int countMails(String user, String platform, long after) {
//        return jdbcTemplate.queryForInt(COUNT, user, platform, after);
//    }
//
//    @Override
//    public void cleanup(final String platformId) {
//        long before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
//        int iterationsLeft = MAX_DELETE_ITERATIONS;
//        boolean running = true;
//        while (iterationsLeft > 0 && running) {
//            iterationsLeft--;
//            final List<Map<String, Object>> entitiesToDelete =
//                    jdbcTemplate.queryForList(CLEANUP_SELECT, before, platformId, DELETE_BULK_SIZE);
//            if (entitiesToDelete.size() == 0) {
//                // We're done unexpectedly early. (Still, this is possible due to replication lag or so. No problem
//                // about it.)
//                return;
//            }
//
//            jdbcTemplate.batchUpdate(
//                    DELETE,
//                    new BatchPreparedStatementSetter() {
//                        public void setValues(PreparedStatement ps, int i) throws SQLException {
//                            Map<String, Object> m = entitiesToDelete.get(i);
//                            String userId = m.get("userId").toString();
//                            String received = m.get("received").toString();
//
//                            ps.setString(1, userId);
//                            ps.setString(2, platformId);
//                            ps.setString(3, received);
//                        }
//
//                        public int getBatchSize() {
//                            return entitiesToDelete.size();
//                        }
//                    });
//
//            running = entitiesToDelete.size() == DELETE_BULK_SIZE;
//            try {
//                Thread.sleep(250);
//            } catch (Exception ex) {
//
//            }
//        }
//        if (iterationsLeft == 0) {
//            LOG.warn("There were more than {} volume records, will delete remaining in next run",
//                    MAX_DELETE_ITERATIONS * DELETE_BULK_SIZE);
//        }
//    }
}
