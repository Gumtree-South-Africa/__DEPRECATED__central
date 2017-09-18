package com.ecg.replyts.core.webapi.control.health;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NoOpHealthCommand extends AbstractHealthCommand {

    private final String name;

    NoOpHealthCommand(String name) {
        this.name = name;
    }

    @Override
    public ObjectNode execute() {
        return status(Status.DOWN);
    }

    @Override
    public String name() {
        return name;
    }
}
