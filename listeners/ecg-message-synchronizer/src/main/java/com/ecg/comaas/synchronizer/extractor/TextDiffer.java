package com.ecg.comaas.synchronizer.extractor;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * COPIED FROM EBAYK MESSAGECENTER TO BE ABLE TO PARSE TEMPLATES.
 */
public class TextDiffer {

    private static final Logger LOG = LoggerFactory.getLogger(TextDiffer.class);

    private static final Pattern NEWLINE = Pattern.compile("(\r?\n)+");
    private static final Pattern EMPTY_SPACE = Pattern.compile(" +");
    private static final Pattern QUOTES = Pattern.compile("[>]+[\\s]*");

    private Map<String, List<NGram>> indexedNgramsOfText;

    private List<NGram> ngramsOfText;

    private Map<NGramMatch, NGramMatch> indexedMatches = new TreeMap<>(Comparator.comparing(NGramMatch::getnGramMatchIndex));

    private Map<NGram, Integer> matchOccurrenceIndex = new LinkedHashMap<>();
    private Integer succedingMatchesToMatchPhrase;

    private EbaykMessagesResponseFactory.DiffInput diffInput;

    public TextDiffer(EbaykMessagesResponseFactory.DiffInput diffInput) {
        this.diffInput = diffInput;
        this.indexedNgramsOfText = new LinkedHashMap<>();
        this.ngramsOfText = initNGrams(diffInput.getText());
    }

    public TextDiffer(EbaykMessagesResponseFactory.DiffInput diffInput, int succedingMatchesToMatchPhrase) {
        this.diffInput = diffInput;
        this.indexedNgramsOfText = new LinkedHashMap<>();
        this.ngramsOfText = initNGrams(diffInput.getText());
        this.succedingMatchesToMatchPhrase = succedingMatchesToMatchPhrase;
    }

    private List<NGram> initNGrams(String originalText) {
        long start = System.currentTimeMillis();

        int positionCounter = 0;

        String target = QUOTES.matcher(originalText).replaceAll(" ");
        target = NEWLINE.matcher(target).replaceAll("\n");
        target = EMPTY_SPACE.matcher(target).replaceAll(" ");
        List<NGram> nGrams = new ArrayList<>();
        Iterable<String> newlineSplitted = Splitter.on("\n").split(target);
        for (String newlineSection : newlineSplitted) {

            List<String> emptySpaceSplitted = Lists.newArrayList(Splitter.on(" ").split(newlineSection.trim()).iterator());
            for (int i = 0; i < emptySpaceSplitted.size(); i++) {

                if (mailClientGeneratedText(emptySpaceSplitted.get(i))) {
                    continue;
                }

                if (i == emptySpaceSplitted.size() - 1) {
                    positionCounter = addNgram(positionCounter, nGrams, "\n", emptySpaceSplitted.get(i));
                } else {
                    positionCounter = addNgram(positionCounter, nGrams, " ", emptySpaceSplitted.get(i));
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        LOG.trace("initNGrams() for {}, Duration: {}", diffInput, duration);

        return nGrams;
    }

    private int addNgram(int positionCounter, List<NGram> nGrams, String delimiter, String text) {
        if (text.contains(".") && !text.contains("...") && !text.contains("@")) {
            ArrayList<String> splitted = Lists.newArrayList(Splitter.on(".").split(text));
            for (int j = 0; j < splitted.size(); j++) {

                if (Strings.isNullOrEmpty(splitted.get(j))) {
                    continue;
                }
                NGram ngram;
                if (j == splitted.size() - 1) {
                    ngram = new NGram(splitted.get(j), delimiter, positionCounter);
                } else {
                    ngram = new NGram(splitted.get(j), "." + delimiter, positionCounter);
                }
                nGrams.add(ngram);
                addToIndex(ngram);
                positionCounter++;
            }

        } else {

            NGram ngram = new NGram(text, delimiter, positionCounter);
            nGrams.add(ngram);
            addToIndex(ngram);
            positionCounter++;
        }
        return positionCounter;
    }

    private void addToIndex(NGram ngram) {
        if (indexedNgramsOfText.get(ngram.getTextContent()) == null) {
            List<NGram> list = Lists.newArrayList(ngram);
            indexedNgramsOfText.put(ngram.getTextContent(), list);
        } else {
            indexedNgramsOfText.get(ngram.getTextContent()).add(ngram);
        }
    }

    public List<NGram> getNgramsOfText() {
        return ngramsOfText;
    }


    public void findMatch(NGram ngramToMatch) {
        boolean addedToOccurence = false;

        if (!indexedNgramsOfText.containsKey(ngramToMatch.getTextContent())) {
            return;
        }

        for (NGram nGram : indexedNgramsOfText.get(ngramToMatch.getTextContent())) {
            NGramMatch matchedNGram = new NGramMatch(ngramToMatch, nGram.position);

            // find better way as Map<NGram, NGram> just for O(1) + lookup
            if (!indexedMatches.containsKey(matchedNGram)) {
                indexedMatches.put(matchedNGram, matchedNGram);
            }
            if (!addedToOccurence) {
                if (matchOccurrenceIndex.containsKey(ngramToMatch)) {
                    Integer occurrence = matchOccurrenceIndex.get(ngramToMatch);
                    matchOccurrenceIndex.put(ngramToMatch, occurrence + 1);
                } else {
                    matchOccurrenceIndex.put(ngramToMatch, 1);
                }
                addedToOccurence = true;
            }
        }

    }

    public Map<NGramMatch, NGramMatch> getIndexedMatches() {
        return indexedMatches;
    }

    public TextCleanerResult cleanupMatches() {
        long start = System.currentTimeMillis();

        Map<Integer, NGramMatch> indexedFilteredMatches = filter(new ArrayList<>(indexedMatches.keySet()));

        StringBuilder b = new StringBuilder();

        int nGramSizeCounter = 0;
        for (int i = 0; i < ngramsOfText.size(); i++) {

            if (!indexedFilteredMatches.containsKey(i) && !escapedWhitespace(ngramsOfText.get(i))) {
                if (ngramsOfText.get(i).isHtmlLink()) {
                    b.append(ngramsOfText.get(i).serializeBack().trim());
                    for (int j = 1; i + j < ngramsOfText.size(); j++) {
                        b.append(ngramsOfText.get(i + j).serializeBack().trim());
                        if (!ngramsOfText.get(i + j).serializeBack().trim().endsWith(".")) {
                            i = i + j;
                            break;
                        }
                    }
                } else {
                    b.append(ngramsOfText.get(i).serializeBack());
                }
                nGramSizeCounter++;
            }
        }

        LOG.debug("cleanupMatches() for {} ngrams: {}, cleanup-candidates: {}, cleaned-up: {}, Duration: {}",
                diffInput,
                ngramsOfText.size(),
                indexedMatches.size(),
                ngramsOfText.size() - nGramSizeCounter,
                System.currentTimeMillis() - start);

        return new TextCleanerResult(b.toString().trim());
    }

    private boolean escapedWhitespace(NGram nGram) {
        if (nGram.getTextContent().contains("\\t"))
            return true;
        if (nGram.getTextContent().contains("\\n"))
            return true;
        return false;
    }

    private boolean mailClientGeneratedText(String text) {
        return text.length() > 500 || text.contains("____________");
    }

    private Map<Integer, NGramMatch> filter(List<NGramMatch> matches) {

        // if not explicitly set guess the best phrase-size, it correlates with overall matches
        if (succedingMatchesToMatchPhrase == null) {
            if (matches.size() == 1) {
                succedingMatchesToMatchPhrase = 1;
            } else if (matches.size() == 2) {
                succedingMatchesToMatchPhrase = 2;
            } else if (matches.size() == 3) {
                succedingMatchesToMatchPhrase = 3;
            } else {
                succedingMatchesToMatchPhrase = 4;
            }
        }

        Map<Integer, NGramMatch> indexedFilteredMatches = new LinkedHashMap<Integer, NGramMatch>();
        Map<NGram, Integer> occurrenceMap2 = new LinkedHashMap<NGram, Integer>();

        for (int i = 0; i < matches.size() - (succedingMatchesToMatchPhrase - 1); i++) {
            int matchedIndizes = 1;
            for (int j = 1; j < succedingMatchesToMatchPhrase; j++) {
                NGramMatch next = matches.get(i + j);
                if (matches.get(i).getnGramMatchIndex() + j == next.getnGramMatchIndex()) {
                    matchedIndizes++;
                }
            }

            if (matchedIndizes == succedingMatchesToMatchPhrase) {
                for (int j = 0; j < succedingMatchesToMatchPhrase; j++) {

                    NGramMatch matchToAdd = matches.get(i + j);

                    // better explain why this is done
                    if (!indexedFilteredMatches.containsKey(matchToAdd.getnGramMatchIndex())) {
                        if (occurrenceMap2.containsKey(matchToAdd.getnGram())) {
                            occurrenceMap2.put(matchToAdd.getnGram(), occurrenceMap2.get(matchToAdd.getnGram()) + 1);
                        } else {
                            occurrenceMap2.put(matchToAdd.getnGram(), 1);
                        }
                    }
                    indexedFilteredMatches.put(matchToAdd.getnGramMatchIndex(), matchToAdd);

                    Integer occurrences = matchOccurrenceIndex.get(matchToAdd.getnGram());

                    int numMatchesFromFiltered2 = occurrenceMap2.get(matchToAdd.getnGram());
                    if (numMatchesFromFiltered2 > occurrences) {
                        removeFirstNGram(indexedFilteredMatches, occurrenceMap2, matchToAdd);
                    }
                }
            }


        }

        return indexedFilteredMatches;
    }

    private void removeFirstNGram(Map<Integer, NGramMatch> filtered, Map<NGram, Integer> occurrenceMap2, NGramMatch match) {

        Integer indexToRemove = null;
        for (NGramMatch nGramMatch : filtered.values()) {
            if (nGramMatch.getnGram().equals(match.getnGram())) {
                indexToRemove = nGramMatch.getnGramMatchIndex();
                break;
            }
        }

        if (indexToRemove != null) {
            filtered.remove(indexToRemove);
            occurrenceMap2.put(match.getnGram(), occurrenceMap2.get(match.getnGram()) - 1);
        }
    }


    public static class NGram {

        private final String textContent;
        private final String emptySpaceSuffix;
        private final int position;

        public NGram(String textContent, String emptySpaceSuffix, int position) {
            this.textContent = mapTextContent(textContent);
            this.emptySpaceSuffix = emptySpaceSuffix;
            this.position = position;
        }

        public boolean isHtmlLink() {
            return getTextContent().startsWith("www") || getTextContent().startsWith("http");

        }

        public String getTextContent() {
            return textContent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NGram nGram = (NGram) o;

            // we only equals() on textContent as emptySpaceSuffix is too fragile (mail clients
            // are changing them)
            if (!textContent.equals(nGram.textContent)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = 31 * textContent.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("NGram{");
            sb.append("textContent='").append(textContent).append('\'');
            sb.append(", emptySpaceSuffix='").append(emptySpaceSuffix).append('\'');
            sb.append(", position=").append(position);
            sb.append('}');
            return sb.toString();
        }

        public String serializeBack() {
            return textContent + emptySpaceSuffix;
        }

        private String mapTextContent(String textContent) {
            // fix ugly mail clients which are switching currency symbols :(
            // as currency symbols often are part of price negotiation fix this here
            if (textContent.contains("\\u00a4")) {
                return textContent.replaceAll("\\\\u00a4", "\\\\u20ac");
            }
            if (textContent.contains("\\u0080")) {
                return textContent.replaceAll("\\\\u0080", "\\\\u20ac");
            }
            return textContent;
        }
    }


    public static class NGramMatch {

        private final NGram nGram;
        private final Integer nGramMatchIndex;

        public NGramMatch(NGram nGram, Integer nGramMatchIndex) {
            this.nGram = nGram;
            this.nGramMatchIndex = nGramMatchIndex;
        }

        public NGram getnGram() {
            return nGram;
        }

        public Integer getnGramMatchIndex() {
            return nGramMatchIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NGramMatch that = (NGramMatch) o;

            if (!nGram.equals(that.nGram)) return false;
            if (!nGramMatchIndex.equals(that.nGramMatchIndex)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = nGram.hashCode();
            result = 31 * result + nGramMatchIndex.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("NGramMatch{");
            sb.append("nGram=").append(nGram);
            sb.append(", nGramMatchIndex=").append(nGramMatchIndex);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class TextCleanerResult {

        public static class ProfilingInfo {

            private long cleanupTimeMillis;
            private final long initNgrams;
            private long ngramScanTimeMillis;
            private long ngramApplyTimeMillis;

            public ProfilingInfo(long cleanupTimeMillis, long initNgrams, long ngramScanTimeMillis, long ngramApplyTimeMillis) {
                this.cleanupTimeMillis = cleanupTimeMillis;
                this.initNgrams = initNgrams;
                this.ngramScanTimeMillis = ngramScanTimeMillis;
                this.ngramApplyTimeMillis = ngramApplyTimeMillis;
            }

            public long getCleanupTimeMillis() {
                return cleanupTimeMillis;
            }

            public long getNgramScanTimeMillis() {
                return ngramScanTimeMillis;
            }

            public long getNgramApplyTimeMillis() {
                return ngramApplyTimeMillis;
            }

            public long getInitNgrams() {
                return initNgrams;
            }

            public long overallTime() {
                return cleanupTimeMillis + ngramScanTimeMillis + ngramApplyTimeMillis + initNgrams;
            }

        }

        private final String cleanupResult;
        private ProfilingInfo profilingInfo;


        public TextCleanerResult(String cleanupResult) {
            this.cleanupResult = cleanupResult;
        }

        public void setProfilingInfo(ProfilingInfo profilingInfo) {
            this.profilingInfo = profilingInfo;
        }

        public ProfilingInfo getProfilingInfo() {
            return profilingInfo;
        }

        public String getCleanupResult() {
            return cleanupResult;
        }
    }
}