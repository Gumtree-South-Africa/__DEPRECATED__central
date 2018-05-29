package com.ecg.comaas.mde.postprocessor.demandreporting.client.filtered;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.DemandReport;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.ReadingDemandReportingClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ReportingClientClassifierDecoratorTest {

    private final String publisherKey1 = "mesa-api-user1";

    private final String publisherKey2 = "MESA-api-user2";

    private final String publisherKey3 = "publisher3";

    private final String referrerKey1 = "referrer1";

    private final String referrerKey2 = "referrer2";

    private final String referrerKey3 = "referrer3";

    private final String vipViewEvent = "vip_view";

    private final String parkingEvent = "parking";

    private final String homepageEvent = "homepage_impression";

    private final String month8 = "2013-08";

    private final String day12 = month8 + "-12";

    private final String day13 = month8 + "-13";

    private final Set<String> eventTypes = new ImmutableSet.Builder<String>().add(vipViewEvent).add(parkingEvent)
            .build();

    private final Set<String> timeFields = new ImmutableSet.Builder<String>().add(day12).add(day13).build();

    //

    private ReadingDemandReportingClient readingDemandReportingClientMock;

    private DemandKeyReviewer demandKeyReviewerMock;

    //

    private ReadingDemandReportingClient reportingClientClassifierDecorator;

    @Before
    public void setUp() {
        readingDemandReportingClientMock = mock(ReadingDemandReportingClient.class);
        demandKeyReviewerMock = mock(DemandKeyReviewer.class);

        reportingClientClassifierDecorator = new ReportingClientClassifierDecorator(readingDemandReportingClientMock,
                demandKeyReviewerMock);

        when(demandKeyReviewerMock.includeDemandFrom(publisherKey1)).thenReturn(true);
        when(demandKeyReviewerMock.includeDemandFrom(publisherKey2)).thenReturn(false);
        when(demandKeyReviewerMock.includeDemandFrom(publisherKey3)).thenReturn(true);
    }

    @Test
    public void testFindByCustomerId() {

        when(readingDemandReportingClientMock.findByCustomerId(vipViewEvent, 1L)).thenReturn(getDemandReport(1));

        Map<Long, DemandReport> result = reportingClientClassifierDecorator.findByCustomerId(vipViewEvent, 1L);

        assertEquals(2, result.size());

        assertEquals(32, result.get(1L).getTotal());
        assertEquals(2, result.get(1L).getPerPublisher().size());
        assertEquals(2, result.get(1L).getPerReferrer().size());

        assertEquals(33, result.get(2L).getTotal());
        assertEquals(2, result.get(2L).getPerPublisher().size());
        assertEquals(3, result.get(2L).getPerReferrer().size());

    }

    @Test
    public void testFindByCustomerIdSetOfEvents() {

        when(readingDemandReportingClientMock.findByCustomerId(eventTypes, 1L)).thenReturn(getDemandReportEventSet());

        Map<String, Map<Long, DemandReport>> result = reportingClientClassifierDecorator.findByCustomerId(eventTypes,
            1L);

        assertEquals(2, result.size());

        assertEquals(32, result.get(vipViewEvent).get(1L).getTotal());
        assertEquals(2, result.get(vipViewEvent).get(1L).getPerPublisher().size());
        assertEquals(2, result.get(vipViewEvent).get(1L).getPerReferrer().size());
        assertEquals(33, result.get(vipViewEvent).get(2L).getTotal());
        assertEquals(2, result.get(vipViewEvent).get(2L).getPerPublisher().size());
        assertEquals(3, result.get(vipViewEvent).get(2L).getPerReferrer().size());

        assertEquals(64, result.get(parkingEvent).get(1L).getTotal());
        assertEquals(2, result.get(parkingEvent).get(1L).getPerPublisher().size());
        assertEquals(2, result.get(parkingEvent).get(1L).getPerReferrer().size());
        assertEquals(66, result.get(parkingEvent).get(2L).getTotal());
        assertEquals(2, result.get(parkingEvent).get(2L).getPerPublisher().size());
        assertEquals(3, result.get(parkingEvent).get(2L).getPerReferrer().size());
    }

    @Test
    public void testFindCustomerEvent() {

        when(readingDemandReportingClientMock.findCustomerEvent(homepageEvent, 1L, month8)).thenReturn(
            getCustomerEvent());

        DemandReport result = reportingClientClassifierDecorator.findCustomerEvent(homepageEvent, 1L, month8);

        assertEquals(45, result.getTotal());
    }

    @Test
    public void testFindByAdId() {

        when(readingDemandReportingClientMock.findByAdId(eventTypes, 1L, timeFields)).thenReturn(
            getDemandReportTimeFields());

        Map<String, Map<String, DemandReport>> result = reportingClientClassifierDecorator.findByAdId(eventTypes, 1L,
            timeFields);

        assertEquals(32, result.get(vipViewEvent).get(day12).getTotal());
        assertEquals(2, result.get(vipViewEvent).get(day12).getPerPublisher().size());
        assertEquals(2, result.get(vipViewEvent).get(day12).getPerReferrer().size());
        assertEquals(33, result.get(vipViewEvent).get(day13).getTotal());
        assertEquals(2, result.get(vipViewEvent).get(day13).getPerPublisher().size());
        assertEquals(3, result.get(vipViewEvent).get(day13).getPerReferrer().size());

        assertEquals(64, result.get(parkingEvent).get(day12).getTotal());
        assertEquals(2, result.get(parkingEvent).get(day12).getPerPublisher().size());
        assertEquals(2, result.get(parkingEvent).get(day12).getPerReferrer().size());
        assertEquals(66, result.get(parkingEvent).get(day13).getTotal());
        assertEquals(2, result.get(parkingEvent).get(day13).getPerPublisher().size());
        assertEquals(3, result.get(parkingEvent).get(day13).getPerReferrer().size());

    }

    private Map<Long, DemandReport> getDemandReport(int factor) {
        Map<Long, DemandReport> result = Maps.newHashMap();

        Map<String, Long> publisher = new ImmutableMap.Builder<String, Long>() //
                .put(publisherKey1, 15L * factor) //
                .put(publisherKey2, 11L * factor) //
                .put(publisherKey3, 17L * factor) //
                .build();
        Map<String, Long> referrer = new ImmutableMap.Builder<String, Long>() //
                .put(referrerKey1, 3L * factor) //
                .put(referrerKey2, 7L * factor) //
                .build();
        result.put(1L, new DemandReport.Builder().total(45).perPublisher(publisher).perReferrer(referrer).build());

        publisher = new ImmutableMap.Builder<String, Long>() //
                .put(publisherKey1, 9L * factor) //
                .put(publisherKey2, 15L * factor) //
                .put(publisherKey3, 24L * factor) //
                .build();
        referrer = new ImmutableMap.Builder<String, Long>() //
                .put(referrerKey1, 2L * factor) //
                .put(referrerKey2, 8L * factor) //
                .put(referrerKey3, 4L * factor) //
                .build();
        result.put(2L, new DemandReport.Builder().total(47).perPublisher(publisher).perReferrer(referrer).build());

        return result;

    }

    private Map<String, DemandReport> getDemandReportTimeField(int factor) {
        Map<String, DemandReport> result = Maps.newHashMap();

        Map<String, Long> publisher = new ImmutableMap.Builder<String, Long>() //
                .put(publisherKey1, 15L * factor) //
                .put(publisherKey2, 11L * factor) //
                .put(publisherKey3, 17L * factor) //
                .build();
        Map<String, Long> referrer = new ImmutableMap.Builder<String, Long>() //
                .put(referrerKey1, 3L * factor) //
                .put(referrerKey2, 7L * factor) //
                .build();
        result.put(day12, new DemandReport.Builder().total(45).perPublisher(publisher).perReferrer(referrer).build());

        publisher = new ImmutableMap.Builder<String, Long>() //
                .put(publisherKey1, 9L * factor) //
                .put(publisherKey2, 15L * factor) //
                .put(publisherKey3, 24L * factor) //
                .build();
        referrer = new ImmutableMap.Builder<String, Long>() //
                .put(referrerKey1, 2L * factor) //
                .put(referrerKey2, 8L * factor) //
                .put(referrerKey3, 4L * factor) //
                .build();
        result.put(day13, new DemandReport.Builder().total(47).perPublisher(publisher).perReferrer(referrer).build());

        return result;

    }

    private Map<String, Map<Long, DemandReport>> getDemandReportEventSet() {
        Map<String, Map<Long, DemandReport>> result = Maps.newHashMap();

        result.put(vipViewEvent, getDemandReport(1));
        result.put(parkingEvent, getDemandReport(2));

        return result;

    }

    private Map<String, Map<String, DemandReport>> getDemandReportTimeFields() {
        Map<String, Map<String, DemandReport>> result = Maps.newHashMap();

        result.put(vipViewEvent, getDemandReportTimeField(1));
        result.put(parkingEvent, getDemandReportTimeField(2));

        return result;

    }

    private DemandReport getCustomerEvent() {
        return new DemandReport.Builder().total(45).build();
    }

}
