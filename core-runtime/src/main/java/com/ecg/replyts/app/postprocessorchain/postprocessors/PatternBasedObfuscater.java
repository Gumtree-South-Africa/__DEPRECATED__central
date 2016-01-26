package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PatternBasedObfuscater implements PostProcessor {

    private List<Pattern> patterns = new ArrayList<Pattern>();
    private final RangeComparator rangeComparator = new RangeComparator();


    void addPattern(Pattern pattern) {
        patterns.add(pattern);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();
        List<TypedContent<String>> textParts = outgoingMail.getTextParts(false);
        for (TypedContent<String> part : textParts) {
            List<Range<Integer>> patternRanges = new ArrayList<Range<Integer>>();
            String content = part.getContent();
            findPatternHitRangesInContent(content, patternRanges);
            if (patternRanges.isEmpty()) {
                //no more action for this critter
                continue;
            }
            //merge potentially overlapping hits for several patterns
            patternRanges = mergePatternHitRanges(patternRanges);
            part.overrideContent(stripPatternHitsFromContent(patternRanges, content));
        }
    }

    private void findPatternHitRangesInContent(String content,
                                               List<Range<Integer>> patternRanges) {
        //find all occurences for all patterns and save their positions as ranges
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            int start = 0;
            while (matcher.find(start)) {
                patternRanges.add(Range.closed(matcher.start(), matcher.end()));
                start = matcher.end();
            }
        }
    }

    private List<Range<Integer>> mergePatternHitRanges(List<Range<Integer>> patternRanges) {
        //sort all ranges by their start points ascendingly
        Collections.sort(patternRanges, rangeComparator);
        //merge entries in a stack
        Stack<Range<Integer>> merged = new Stack<Range<Integer>>();
        for (Range<Integer> range : patternRanges) {
            if (merged.isEmpty()) {
                //first element
                merged.push(range);
                continue;
            }
            if (range.isConnected(merged.peek())) {
                range = range.span(merged.pop());
            }
            merged.push(range);
        }
        return new ArrayList<Range<Integer>>(merged);
    }

    private String stripPatternHitsFromContent(List<Range<Integer>> patternRanges, String content) {
        StringBuilder builder = new StringBuilder();
        int start = 0;
        int end;
        for (Range<Integer> range : patternRanges) {
            end = range.lowerEndpoint().intValue();
            //... and add 'em to builder ...
            builder.append(content.substring(start, end));
            start = range.upperEndpoint().intValue();
        }
        //... including the final piece.
        builder.append(content.substring(start));
        return builder.toString();
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    private static class RangeComparator implements Comparator<Range<Integer>>, Serializable {

        private static final long serialVersionUID = 1l;

        @Override
        public int compare(Range<Integer> range1, Range<Integer> range2) {
            return range1.lowerEndpoint().intValue() - range2.lowerEndpoint().intValue();
        }
    }
}
