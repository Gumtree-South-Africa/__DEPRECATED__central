package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.model.ParticipantRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParticipantResponse {

    @ApiModelProperty(required = true)
    private final String userId;
    @ApiModelProperty(required = true)
    private final ParticipantRole role;
    private final String email;
    private final String name;

    public ParticipantResponse(String userId, String name, String email, ParticipantRole role) {
        this.userId = userId;
        this.name = name == null ? "" : name;
        this.email = email;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public ParticipantRole getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantResponse that = (ParticipantResponse) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(name, that.name)
                && Objects.equals(email, that.email)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, name, email, role);
    }
}