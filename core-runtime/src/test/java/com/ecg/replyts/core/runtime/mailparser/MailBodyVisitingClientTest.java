package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailBodyVisitingClientTest {

    @Mock
    private Message mockMail;

    private EntityListingMailBodyVisitor visitor = new EntityListingMailBodyVisitor();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void visitsBodyPartOfSimpleMail() throws Exception {
        SingleBody simpleBody = mock(SingleBody.class);
        when(mockMail.getBody()).thenReturn(simpleBody);
        new MailBodyVisitingClient(mockMail).visit(visitor);

        assertEquals(1, visitor.getItems().size());
        assertEquals(simpleBody, visitor.getItems().get(0));
    }

    /**
     * mail looks like:
     * <pre>
     *     Mail
     *     + Multipart
     *       + Singlebody 1
     *       + Singlebody 2
     *       + Singlebody 3
     * </pre>
     */
    @Test
    public void visitsAllBodiesOfMultipart() throws Exception {
        Multipart mp = mock(Multipart.class);
        SingleBody s1 = mock(SingleBody.class);
        SingleBody s2 = mock(SingleBody.class);
        SingleBody s3 = mock(SingleBody.class);
        Entity c1 = mock(Entity.class);
        Entity c2 = mock(Entity.class);
        Entity c3 = mock(Entity.class);
        when(c1.getBody()).thenReturn(s1);
        when(c2.getBody()).thenReturn(s2);
        when(c3.getBody()).thenReturn(s3);
        when(mp.getBodyParts()).thenReturn(asList(c1, c2, c3));
        when(mockMail.getBody()).thenReturn(mp);

        new MailBodyVisitingClient(mockMail).visit(visitor);

        assertEquals(3, visitor.getItems().size());
        assertEquals(s1, visitor.getItems().get(0));
        assertEquals(s2, visitor.getItems().get(1));
        assertEquals(s3, visitor.getItems().get(2));

    }

    /**
     * mail looks like:
     * <pre>
     *      Mail
     *      +Multipart
     *        + Multipart
     *          + Singlebody 1
     *          + Singlebody 2
     *        + Singlebody 3
     * </pre>
     */
    @Test
    public void walksDepthFirstOnHierarchicalMultiparts() throws Exception {
        Multipart rootMultipart = mock(Multipart.class);
        Multipart subMultipart = mock(Multipart.class);
        Entity subMultipartEntity = mock(Entity.class);
        when(subMultipartEntity.getBody()).thenReturn(subMultipart);
        SingleBody s1 = mock(SingleBody.class);
        SingleBody s2 = mock(SingleBody.class);
        SingleBody s3 = mock(SingleBody.class);
        Entity c1 = mock(Entity.class);
        Entity c2 = mock(Entity.class);
        Entity c3 = mock(Entity.class);
        when(c1.getBody()).thenReturn(s1);
        when(c2.getBody()).thenReturn(s2);
        when(c3.getBody()).thenReturn(s3);
        when(rootMultipart.getBodyParts()).thenReturn(asList(subMultipartEntity, c3));
        when(subMultipart.getBodyParts()).thenReturn(asList(c1, c2));
        when(mockMail.getBody()).thenReturn(rootMultipart);

        new MailBodyVisitingClient(mockMail).visit(visitor);

        assertEquals(3, visitor.getItems().size());
        assertEquals(s1, visitor.getItems().get(0));
        assertEquals(s2, visitor.getItems().get(1));
        assertEquals(s3, visitor.getItems().get(2));
    }
}
