package com.ecg.messagecenter.kjca.pushmessage.exception;

import java.io.IOException;

public class APIException extends IOException {
    public APIException(String message) {
        super(message);
    }
}
