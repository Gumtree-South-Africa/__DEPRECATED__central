package com.ecg.replyts.core.runtime.mailparser;


import com.google.common.collect.ImmutableList;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;

/**
 * client for structured mail visitors that will iterate every part of a Mime4J Mail recursively. Visitors will only see
 * the leaf nodes of a mail (all mail bodies that are not multipart or do not have nested parts).
 */
public class MailBodyVisitingClient {

    private final ImmutableList<Entity> visitableElements;

    public MailBodyVisitingClient(Entity mail) {
        ImmutableList.Builder<Entity> flattenedEntities = ImmutableList.builder();
        expand(mail, flattenedEntities);
        visitableElements = flattenedEntities.build();
    }

    /**
     * will take the visitor on for a walk around the mail. On this guided tour the visitor we will see all
     * content (not the boilerplate overhead) of the mail. For carrying and older visitors, please have caution
     * as our ride will be depth-first.
     * <br/>
     * If anyone ever reads this javadoc: I would be extremely happy to see it on <a href="http://www.thedailywtf.com">The Daily WTF</a>
     */
    public <T extends MailBodyVisitor> T visit(T visitor) {
        for (Entity e : visitableElements) {
            visitor.visit(e, (SingleBody) e.getBody());
        }
        return visitor;
    }

    private void expand(Entity source, ImmutableList.Builder<Entity> flattenedEntities) {
        EntityFlavour flavour = getFlavour(source);
        switch (flavour) {
            case ContainsAnotherEntity:
                expand((Entity) source.getBody(), flattenedEntities);
                break;
            case ContainsSingleBody:
                flattenedEntities.add(source);
                break;
            case Multipart:
                for (Entity child : ((Multipart) source.getBody()).getBodyParts()) {
                    expand(child, flattenedEntities);
                }
                break;
        }
    }

    private EntityFlavour getFlavour(Entity e) {
        Body body = e.getBody();

        if (body instanceof Entity) {
            return EntityFlavour.ContainsAnotherEntity;
        }
        if (body instanceof Multipart) {
            return EntityFlavour.Multipart;
        }
        return EntityFlavour.ContainsSingleBody;

    }


}
