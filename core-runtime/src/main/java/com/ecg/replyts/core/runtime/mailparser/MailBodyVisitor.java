package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;

/**
 * Visitor interface for {@link MailBodyVisitingClient} that will visit all leaf bodies of a mail. Visitor
 * implementations will not need to take care about the recursive format of mails or the analyzing mail bodies for their
 * type.
 */
interface MailBodyVisitor {
    /**
     * invoked by the client to visit all entities that carry actual mail data and are not just
     * structural overhead.
     * <p/>
     * <p/>
     *
     * @param e    entity containing headers and body information of the visited element
     * @param body shorthand for <code>(SingleBody)e.getBody()</code> - the entities body.
     */
    void visit(Entity e, SingleBody body);
}
