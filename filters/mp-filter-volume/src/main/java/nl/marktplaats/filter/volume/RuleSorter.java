package nl.marktplaats.filter.volume;


import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RuleSorter implements Comparator<VolumeRule> {

    public List<VolumeRule> orderRules(VolumeFilterConfiguration config) {
        List<VolumeRule> rules = new ArrayList<VolumeRule>(config.getConfig());
        Collections.sort(rules, this);
        return Collections.unmodifiableList(rules);
    }


    @Override
    public int compare(VolumeRule o1, VolumeRule o2) {
        return o2.getScore() - o1.getScore();
    }
}
