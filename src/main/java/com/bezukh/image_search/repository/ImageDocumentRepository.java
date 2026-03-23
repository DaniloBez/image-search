package com.bezukh.image_search.repository;

import com.bezukh.image_search.model.ImageDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageDocumentRepository extends JpaRepository<ImageDocument, Long> {
}
