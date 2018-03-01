package com.ecg.replyts.app.postprocessorchain;

public interface ContentOverridingPostProcessor extends PostProcessor {

    String overrideContent(String content);
}
