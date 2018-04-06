package com.ecg.comaas.core.filter.bankaccount;

import com.google.common.collect.Lists;

import java.util.List;

public class NumberStreamExtractor {

    private final int maxNumberOfSkippableCharacters;
    private final String mailBody;


    public NumberStreamExtractor(int maxNumberOfSkippableCharacters, String mailBody) {
        this.maxNumberOfSkippableCharacters = maxNumberOfSkippableCharacters;
        this.mailBody = mailBody;
    }

    private List<String> items = Lists.newArrayList();
    private StringBuilder s;
    private int skippedCharacters;
    
    public NumberStream extractStream() {

        for (char c : mailBody.toCharArray()) {
            if(Character.isDigit(c)) {
                appendCurrent(c);
            } else if(Character.isWhitespace(c)) {
                // generally: skip whitespace
            } else {
                skipCharacter();
            }
        }
        completeGroup();
        return new NumberStream(items);
    }

    private void skipCharacter() {
        if(++skippedCharacters > maxNumberOfSkippableCharacters) {
            completeGroup();
        }
    }

    private void completeGroup() {
        if(s!=null){
            items.add(s.toString());
            s = null;
        }
    }

    private void appendCurrent(char c) {
        if(s== null) {
            startNewNumberGroup();
        }
        skippedCharacters = 0;
        s.append(c);
    }

    private void startNewNumberGroup() {
        s = new StringBuilder();
    }
}
