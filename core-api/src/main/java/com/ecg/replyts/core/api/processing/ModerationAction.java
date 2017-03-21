package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ModerationAction {
    private final Optional<String> editor;
    private final ModerationResultState moderationResultState;

    public ModerationAction(ModerationResultState moderationResultState, Optional<String> editor) {
        Preconditions.checkNotNull(moderationResultState);
        Preconditions.checkNotNull(editor);
        this.editor = editor;
        this.moderationResultState = moderationResultState;
    }

    public Optional<String> getEditor() {
        return editor;
    }

    public ModerationResultState getModerationResultState() {
        return moderationResultState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModerationAction that = (ModerationAction) o;

        if (!editor.equals(that.editor)) return false;
        if (moderationResultState != that.moderationResultState) return false;

        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("editor", editor)
                .add("moderationResultState", moderationResultState)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(editor, moderationResultState);
    }
}
