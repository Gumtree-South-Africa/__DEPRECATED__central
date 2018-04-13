package com.ecg.comaas.mp.filter.volume;


import com.ecg.comaas.mp.filter.volume.VolumeFilterConfiguration.VolumeRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RuleSorter implements Comparator<VolumeRule> {
    List<VolumeRule> orderRules(VolumeFilterConfiguration config) {
        List<VolumeRule> rules = new ArrayList<>(config.getConfig());
        rules.sort(this);
        return Collections.unmodifiableList(rules);
    }


    @Override
    public int compare(VolumeRule o1, VolumeRule o2) {
        return o2.getScore() - o1.getScore();
    }
}
