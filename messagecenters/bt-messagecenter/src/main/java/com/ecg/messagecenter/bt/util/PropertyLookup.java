package com.ecg.messagecenter.bt.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLookup {
	
	private static int maxDays = 180;
	private static String userSnapshotKernelUrl = "http://kernel.phx.ops.prod.bt.ecg.so/api/users/snapshots";
	
	@Autowired
	public PropertyLookup(@Value("${replyts.maxConversationAgeDays}") int maxDays,
						  @Value("${replyts.user.snapshot.kernel.url}") String userSnapshotKernelUrl) {
		PropertyLookup.maxDays = maxDays;
		PropertyLookup.userSnapshotKernelUrl = userSnapshotKernelUrl;
    }
	
	public static int getMaxDays(){
		return maxDays;
	}
	
	public static String getuserSnapshotKernelUrl() {
		return userSnapshotKernelUrl;
	}
}
