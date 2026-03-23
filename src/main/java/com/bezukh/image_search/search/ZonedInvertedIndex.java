package com.bezukh.image_search.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZonedInvertedIndex implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> documentNames = new ArrayList<>();
    private final Map<Integer, Double> documentLengths = new ConcurrentHashMap<>();
    private Map<String, List<TermMetadata>> index;

    public void addDocumentName(String name) {
        documentNames.add(name);
    }

    public void addDocumentLength(int documentId, double length) {
        documentLengths.put(documentId, length);
    }

    public double getDocumentLength(int documentId) {
        return documentLengths.get(documentId);
    }

    public int getDocumentCount() {
        return documentNames.size();
    }

    public List<TermMetadata> getDocuments(String term) {
        return index.getOrDefault(term, new ArrayList<>());
    }

    public String getDocumentName(int id) {
        if (id >= 0 && id < documentNames.size()) {
            return documentNames.get(id);
        }
        return "Unknown Document";
    }

    public void setData(Map<String, List<TermMetadata>> data) {
        data.values().forEach(Collections::sort);
        this.index = new HashMap<>(data);
    }
}
