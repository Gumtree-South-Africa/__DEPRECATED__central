package com.ecg.replyts.core.webapi.util;

import com.google.common.io.LineReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EchoResponseGenerator {

    private static final String QUOTES_FILE = "chucknorris.txt";
    private static final String DEFAULT_RESPONSE = "pong";

    private static final EchoResponseGenerator INSTANCE = new EchoResponseGenerator(QUOTES_FILE);

    private final List<String> quotes;
    private final Random random;

    public EchoResponseGenerator(String fileName) {
        quotes = loadQuotes(fileName);
        random = new Random(System.nanoTime());
    }

    public static String defaultEchoResponse() {
        return DEFAULT_RESPONSE;
    }

    public static String randomEchoResponse() {
        return INSTANCE.randomQuote();
    }

    public String randomQuote() {
        return quotes.get(random.nextInt(quotes.size()));
    }

    private List<String> loadQuotes(String fileName) {
        try {
            return readLines(fileName);
        } catch (IOException e) {
            return Collections.singletonList(DEFAULT_RESPONSE);
        }
    }

    private List<String> readLines(String fileName) throws IOException {
        List<String> lines = new ArrayList<String>();
        LineReader lineReader = new LineReader(new InputStreamReader(getClass().getResourceAsStream(fileName)));
        for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
            lines.add(line);
        }
        return lines;
    }

}
