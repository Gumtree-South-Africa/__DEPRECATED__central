package com.ecg.replyts.core.api.model.user.event;

import com.ecg.replyts.core.api.util.Assert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class BlockedUserEvent extends UserEvent {
    private final String blockerId;
    private final String blockeeId;
    private final BlockAction blockAction;

    @JsonCreator
    public BlockedUserEvent(@JsonProperty("blockerId") String blockerId,
                            @JsonProperty("blockeeId") String blockeeId,
                            @JsonProperty("blockAction") BlockAction blockAction) {
        this.blockerId = blockerId;
        this.blockeeId = blockeeId;
        this.blockAction = blockAction;
        Assert.notNull(blockeeId);
        Assert.notNull(blockerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BlockedUserEvent that = (BlockedUserEvent) o;
        return blockAction == that.blockAction &&
                Objects.equals(blockerId, that.blockerId) &&
                Objects.equals(blockeeId, that.blockeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), blockerId, blockeeId, blockAction);
    }

    public String getBlockerId() {
        return blockerId;
    }

    public String getBlockeeId() {
        return blockeeId;
    }

    public BlockAction getBlockAction() {
        return blockAction;
    }
}
