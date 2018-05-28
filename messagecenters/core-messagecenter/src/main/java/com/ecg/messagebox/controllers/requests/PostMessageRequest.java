package com.ecg.messagebox.controllers.requests;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class PostMessageRequest {

    @NotNull(message = "Message text cannot be null")
    public String message;

    public Map<String, String> metadata = new HashMap<>();
}
