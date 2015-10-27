package nl.marktplaats.postprocessor.anonymizebody;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by reweber on 19/10/15
 */
public class AnonymiseEmailPostProcessorConfig {

    private List<String> patterns = new ArrayList<>();

    public AnonymiseEmailPostProcessorConfig(List<String> patterns) {
        this.patterns = patterns;
    }

    public List<String> getPatterns() {
        return patterns;
    }
}
