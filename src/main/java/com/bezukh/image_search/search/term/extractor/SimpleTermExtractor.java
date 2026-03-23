package com.bezukh.image_search.search.term.extractor;

import opennlp.tools.stemmer.PorterStemmer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SimpleTermExtractor implements TermExtractor {
    private final PorterStemmer stemmer;
    private final Pattern pattern;

    public SimpleTermExtractor() {
        this.stemmer = new PorterStemmer();
        this.pattern = Pattern.compile("[a-zA-Z0-9]+([\\-.`'@+][a-zA-Z0-9]+)*");
    }

    @Override
    public Stream<String> extractTerms(String text) {
        ArrayList<String> terms = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String rawWord = matcher.group();
            String stemmedWord = stemmer.stem(rawWord.toLowerCase());
            terms.add(stemmedWord);
        }
        return terms.stream();
    }
}
