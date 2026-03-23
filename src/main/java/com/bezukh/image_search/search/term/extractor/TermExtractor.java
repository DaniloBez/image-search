package com.bezukh.image_search.search.term.extractor;

import java.util.stream.Stream;

public interface TermExtractor {
    Stream<String> extractTerms(String text);
}
