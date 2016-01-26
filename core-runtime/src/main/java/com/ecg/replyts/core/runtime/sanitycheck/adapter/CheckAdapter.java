package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Result;

import javax.management.ObjectName;


public interface CheckAdapter extends CheckAdapterMBean {

    String getName();

    String getCategory();

    String getSubCategory();

    Result getLatestResult();

    void destroy();

    void setObjectName(ObjectName oname);


}
