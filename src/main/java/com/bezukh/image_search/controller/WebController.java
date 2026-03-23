package com.bezukh.image_search.controller;

import com.bezukh.image_search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final SearchService searchService;

    @GetMapping("/")
    public String indexPage(@RequestParam(value = "q", required = false) String query, Model model) {
        if (query != null && !query.isEmpty()) {
            model.addAttribute("results", searchService.search(query));
            model.addAttribute("query", query);
        } else {
            model.addAttribute("results", List.of());
        }
        return "index";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }
}