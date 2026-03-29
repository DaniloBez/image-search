package com.bezukh.image_search.service;

import com.bezukh.image_search.dto.ImageSearchResultDto;
import com.bezukh.image_search.model.ImageDocument;
import com.bezukh.image_search.repository.ImageDocumentRepository;
import com.bezukh.image_search.search.ZonedInvertedIndex;
import com.bezukh.image_search.search.TermMetadata;
import com.bezukh.image_search.search.term.extractor.SimpleTermExtractor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final ImageDocumentRepository repository;
    private final SimpleTermExtractor termExtractor = new SimpleTermExtractor();
    private ZonedInvertedIndex indexModel = new ZonedInvertedIndex();

    @Value("${app.index.path:./index.bin}")
    private String indexPath;

    private static final byte FILENAME_ZONE = 1;
    private static final byte DESCRIPTION_ZONE = 4;

    @PostConstruct
    public void init() {
        if (Files.exists(Paths.get(indexPath)))
            loadIndex();
        else {
            log.info("Index file not found. Checking database...");
            long count = repository.count();
            if (count > 0)
                rebuildIndex();
            else
                log.info("Database is empty. Initializing empty index.");
        }
    }

    public void rebuildIndex() {
        log.info("Starting index rebuild from database...");
        List<ImageDocument> documents = repository.findAll();

        ZonedInvertedIndex newModel = new ZonedInvertedIndex();
        Map<String, List<TermMetadata>> data = new HashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            ImageDocument doc = documents.get(i);
            int docId = i;
            newModel.addDocumentName(doc.getOriginalFileName());

            Map<String, TermMetadata> localIndex = new HashMap<>();
            processZone(doc.getOriginalFileName(), docId, FILENAME_ZONE, localIndex);
            processZone(doc.getAiDescription(), docId, DESCRIPTION_ZONE, localIndex);

            double documentLength = 0;
            for (TermMetadata metadata : localIndex.values())
                documentLength += Math.pow(1 + Math.log10(metadata.termFrequency()), 2);

            newModel.addDocumentLength(docId, Math.sqrt(documentLength));

            localIndex.forEach((term, metadata) ->
                    data.computeIfAbsent(term, k -> new ArrayList<>()).add(metadata)
            );
        }

        newModel.setData(data);
        this.indexModel = newModel;
        saveIndex();
        log.info("Index rebuild complete. Indexed {} terms across {} documents.", data.size(), documents.size());
    }

    public List<ImageSearchResultDto> search(String query) {
        log.info("Searching for query: {}", query);

        List<String> queryTerms = termExtractor.extractTerms(query).toList();
        if (queryTerms.isEmpty()) return Collections.emptyList();

        Map<Integer, Double> scores = new HashMap<>();
        int N = indexModel.getDocumentCount();

        for (String term : queryTerms) {
            List<TermMetadata> docs = indexModel.getDocuments(term);
            if (docs.isEmpty()) continue;

            double idf = Math.log10((double) N / docs.size());

            for (TermMetadata meta : docs) {
                double tf = 1 + Math.log10(meta.termFrequency());
                double score = tf * idf;

                if ((meta.zoneMask() & FILENAME_ZONE) != 0) score *= 1.2;

                scores.merge(meta.docId(), score, Double::sum);
            }
        }

        List<ImageDocument> allDocs = repository.findAll();

        return scores.entrySet().stream()
                .map(entry -> {
                    int docIdIndex = entry.getKey();
                    double finalScore = entry.getValue() / indexModel.getDocumentLength(docIdIndex);

                    ImageDocument doc = allDocs.get(docIdIndex);

                    return ImageSearchResultDto.builder()
                            .id(doc.getId())
                            .fileName(doc.getOriginalFileName())
                            .displayUrl("/api/images/display/" + doc.getId())
                            .thumbnailUrl("/api/images/display/" + doc.getId() + "/thumb")
                            .aiDescription(doc.getAiDescription())
                            .score(finalScore)
                            .build();
                })
                //.filter(dto -> dto.getScore() > 0.01)
                .sorted(Comparator.comparingDouble(ImageSearchResultDto::getScore).reversed())
                //.limit(20)
                .toList();
    }

    private void processZone(String text, int docId, byte zoneMask, Map<String, TermMetadata> localIndex) {
        if (text == null || text.isEmpty()) return;

        termExtractor.extractTerms(text).forEach(term -> {
            localIndex.merge(term, new TermMetadata(docId, zoneMask, 1),
                    (existing, newMeta) -> new TermMetadata(
                            existing.docId(),
                            (byte) (existing.zoneMask() | newMeta.zoneMask()),
                            existing.termFrequency() + newMeta.termFrequency()
                    )
            );
        });
    }

    private void saveIndex() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexPath))) {
            oos.writeObject(indexModel);
            log.info("Index saved to: {}", indexPath);
        } catch (IOException e) {
            log.error("Failed to save index", e);
        }
    }

    private void loadIndex() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexPath))) {
            this.indexModel = (ZonedInvertedIndex) ois.readObject();
            log.info("Index loaded successfully. Docs: {}", indexModel.getDocumentCount());
        } catch (Exception e) {
            log.error("Failed to load index, starting rebuild", e);
            rebuildIndex();
        }
    }
}