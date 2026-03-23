package com.bezukh.image_search.search;

import java.io.Serializable;

public record TermMetadata(int docId, byte zoneMask, int termFrequency) implements Comparable<TermMetadata>, Serializable {

    @Override
    public int compareTo(TermMetadata other) {
        return Integer.compare(this.docId, other.docId);
    }
}
