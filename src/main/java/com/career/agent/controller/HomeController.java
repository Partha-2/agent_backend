package com.career.agent.controller;

import jakarta.servlet.http.HttpSession;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    @Value("${rapid.api.key}")
    private String rapidApiKey;

    @Value("${groq.api.key}")
    private String groqApiKey;

    /* ================= COMMON ================= */

    private void addDefaults(Model model, Principal principal) {
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("userEmail", principal != null ? principal.getName() : "");
        model.addAttribute("jobs", new ArrayList<>());
        model.addAttribute("error", null);
        model.addAttribute("currentPage", 1);
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String msg, Model model) {
        model.addAttribute("infoMsg", msg);
        model.addAttribute("authenticated", false);
        return "login";
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        addDefaults(model, principal);
        return "dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie c = new Cookie("jwt_token", "");
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
        return "redirect:/login?msg=Logged Out";
    }

    /* ================= SEARCH ================= */

    @RequestMapping(value = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    public String search(
            @RequestParam String role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String datePosted,
            @RequestParam(required = false) MultipartFile resume,
            @RequestParam(defaultValue = "1") int page,
            Model model,
            Principal principal,
            HttpSession session) {

        addDefaults(model, principal);

        if (principal == null) {
            return "redirect:/login?msg=Please sign in first";
        }

        if (role == null || role.isBlank()) {
            model.addAttribute("error", "Please enter a job title.");
            return "dashboard";
        }

        // 🔴 RESET session only on FIRST PAGE
        if (page == 1) {
            session.removeAttribute("jobCache");
        }

        List<Map<String, Object>> allJobs =
                (List<Map<String, Object>>) session.getAttribute("jobCache");

        if (allJobs == null) {
            allJobs = new ArrayList<>();
        }

        try {
            List<Map<String, Object>> newJobs =
                    fetchJSearchUntilFound(role, location, datePosted, "", page);

            // ✅ APPEND instead of overwrite
            allJobs.addAll(newJobs);
            session.setAttribute("jobCache", allJobs);

            model.addAttribute("jobs", allJobs);
            model.addAttribute("currentPage", page);

        } catch (Exception e) {

            List<Map<String, Object>> fallback =
                    fetchRemotivePaged(role, page);

            allJobs.addAll(fallback);
            session.setAttribute("jobCache", allJobs);

            model.addAttribute("jobs", allJobs);
            model.addAttribute("error",
                    "Primary source busy. Showing additional results.");
        }

        return "dashboard";
    }


    /* ================= LOAD MORE (AJAX) ================= */

    @GetMapping("/api/load-more")
    @ResponseBody
    public List<Map<String, Object>> loadMore(
            @RequestParam String role,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") int page) {

        try {
            return fetchJSearchUntilFound(role, location, null, "", page);
        } catch (Exception e) {
            return fetchRemotivePaged(role, page);
        }
    }

    /* ================= JSEARCH LOGIC ================= */

    private List<Map<String, Object>> fetchJSearchUntilFound(
            String role,
            String location,
            String date,
            String resumeText,
            int startPage) {

        int MAX_PAGES = 5; // safety limit

        for (int p = startPage; p <= MAX_PAGES; p++) {
            List<Map<String, Object>> jobs =
                    fetchJSearchJobs(role, location, date, resumeText, p);

            if (!jobs.isEmpty()) {
                return jobs;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchJSearchJobs(
            String role,
            String location,
            String date,
            String resumeText,
            int page) {

        String query = role + " " +
                (location != null && !location.isBlank() ? location : "India");

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString("https://jsearch.p.rapidapi.com/search")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("num_pages", 1);

        if (date != null && !"all".equals(date)) {
            builder.queryParam("date_posted", date);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", rapidApiKey);
        headers.set("X-RapidAPI-Host", "jsearch.p.rapidapi.com");

        ResponseEntity<Map> response =
                new RestTemplate().exchange(
                        builder.build().encode().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

        Map body = response.getBody();
        if (body == null || !body.containsKey("data")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("data");
        List<Map<String, Object>> jobs = new ArrayList<>();

        for (Map r : raw) {
            Map<String, Object> j = new HashMap<>();
            j.put("title", r.get("job_title"));
            j.put("company", r.get("employer_name"));
            j.put("job_city", r.get("job_city"));

            String desc = stripHtml(Objects.toString(r.get("job_description"), ""));
            j.put("desc", desc.length() > 240 ? desc.substring(0, 240) + "..." : desc);

            j.put("url", r.get("job_apply_link"));
            j.put("logo", r.get("employer_logo"));
            j.put("score", 60);
            j.put("reason", "Live job from JSearch");

            jobs.add(j);
        }
        return jobs;
    }

    /* ================= REMOTIVE (PAGED) ================= */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRemotivePaged(String role, int page) {

        String url = "https://remotive.com/api/remote-jobs?search=" + role;
        Map<String, Object> resp = new RestTemplate().getForObject(url, Map.class);

        if (resp == null || !resp.containsKey("jobs")) return Collections.emptyList();

        List<Map<String, Object>> all =
                (List<Map<String, Object>>) resp.get("jobs");

        int pageSize = 10;
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, all.size());

        if (from >= all.size()) return Collections.emptyList();

        List<Map<String, Object>> jobs = new ArrayList<>();

        for (Map r : all.subList(from, to)) {
            Map<String, Object> j = new HashMap<>();
            j.put("title", r.get("title"));
            j.put("company", r.get("company_name"));
            j.put("job_city", "Remote");

            String desc = stripHtml(Objects.toString(r.get("description"), ""));
            j.put("desc", desc.length() > 240 ? desc.substring(0, 240) + "..." : desc);

            j.put("url", r.get("url"));
            j.put("logo", r.get("company_logo"));
            j.put("score", 50);
            j.put("reason", "Live remote job (Remotive)");

            jobs.add(j);
        }
        return jobs;
    }

    /* ================= HELPERS ================= */

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

