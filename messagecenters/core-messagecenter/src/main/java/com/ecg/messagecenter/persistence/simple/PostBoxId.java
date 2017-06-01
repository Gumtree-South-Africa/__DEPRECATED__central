package com.ecg.messagecenter.persistence.simple;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class PostBoxId {
    private final String id;

    private PostBoxId(@Nonnull String id) {
        this.id = checkNotNull(id, "id is null");
    }

    public String asString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBoxId postBoxId = (PostBoxId) o;
        return Objects.equal(id, postBoxId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static PostBoxId fromEmail(String email) {
        return new PostBoxId(checkNotNull(email, "email is null").toLowerCase());
    }
}
