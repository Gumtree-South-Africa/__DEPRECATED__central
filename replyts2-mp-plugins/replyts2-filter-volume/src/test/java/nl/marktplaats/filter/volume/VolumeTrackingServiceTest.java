package nl.marktplaats.filter.volume;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = "classpath:volumefilter-test-context.xml")
//@TestExecutionListeners(listeners = {
//        DependencyInjectionTestExecutionListener.class,
//        DirtiesContextTestExecutionListener.class})
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class VolumeTrackingServiceTest {
//
//    @Autowired
//    private VolumeTrackingServiceJdbc vts;
//
//    @Test
//    public void testTrack() {
//        vts.record("user1@host.com", "ebayk");
//        vts.record("user1@host.com", "ebayk");
//        vts.record("user2@host.com", "ebayk");
//        Assert.assertEquals(2, vts.countMails("user1@host.com", "ebayk", 0));
//        Assert.assertEquals(1, vts.countMails("user2@host.com", "ebayk", 0));
//    }
//
//    @Test
//    public void testTimelyTrack() {
//        vts.record("user3@host.com", 100, "ebayk");
//        vts.record("user3@host.com", 200, "ebayk");
//        vts.record("user3@host.com", 300, "ebayk");
//        Assert.assertEquals(2, vts.countMails("user3@host.com", "ebayk", 150));
//    }
//
//    @Test
//    public void testRuleHarming() {
//
//        final String u = "user4@host.com";
//        final String p = "ebayk";
//
//        for (int i = 0; i < 20; i++) {
//            long ago = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(i);
//            vts.record(u, ago, p);
//        }
//        Assert.assertFalse(vts.violates(u, p, new VolumeRule(20l, TimeUnit.HOURS, 20l, 10)));
//        Assert.assertTrue(vts.violates(u, p, new VolumeRule(4l, TimeUnit.HOURS, 3l, 10)));
//    }
//
//    @Test
//    public void testCleanup() {
//        vts.record("user5@host.com", "ebayk");
//        for (int i = 0; i < 200; i++) {
//            long ago = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(i + 1);
//            vts.record("user5@host.com", ago, "ebayk");
//        }
//        vts.cleanup("ebayk");
//        Assert.assertEquals(1, vts.countMails("user5@host.com", "ebayk", 0));
//
//    }
//

}
