package com.ecg.messagebox.controllers.requests;

import javax.validation.constraints.NotNull;

public class PostMessageRequest {

    @NotNull(message = "Message text cannot be null")
    public String message;
}
