package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;

import java.util.ArrayList;
import java.util.List;

public class EntityListingMailBodyVisitor implements MailBodyVisitor {

    private List<SingleBody> items = new ArrayList<SingleBody>();

    @Override
    public void visit(Entity e, SingleBody body) {
        items.add(body);
    }

    public List<SingleBody> getItems() {
        return items;
    }
}
