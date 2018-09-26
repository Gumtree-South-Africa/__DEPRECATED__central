package com.ecg.replyts.core.api.util;

public class Constants {

    //Most likely we face such warning in the case of loadbalancer/orchestration system misconfiguration
    //The app gets calls/processes msgs after being interrupted
    //see COMAAS-1309/COMAAS-1326/COMAAS-1338/COMAAS-1339
    public static final String INTERRUPTED_WARNING = "The application receives call after shut down signal";
}
