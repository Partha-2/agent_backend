package com.career.agent.controller;

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
import java.net.URI;

@Controller
public class HomeController {

    @Value("${rapid.api.key}")
    private String rapidApiKey;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private void addDefaultAttributes(Model model, Principal principal) {
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("userEmail", (principal != null) ? principal.getName() : "");
        model.addAttribute("jobs", new ArrayList<Map<String, Object>>());
        model.addAttribute("currentPage", 1);
        model.addAttribute("role", "");
        model.addAttribute("location", "");
        model.addAttribute("experience", "all");
        model.addAttribute("datePosted", "all");
        model.addAttribute("resumeActive", false);
        model.addAttribute("resumeFileName", "");
        model.addAttribute("error", null);
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        addDefaultAttributes(model, principal);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String msg, Model model) {
        model.addAttribute("infoMsg", msg);
        model.addAttribute("authenticated", false);
        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie c = new Cookie("jwt_token", "");
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
        return "redirect:/login?msg=Logged Out";
    }

    @RequestMapping(value = "/search", method = { RequestMethod.GET, RequestMethod.POST })
    public String search(@RequestParam(required = false) String role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String datePosted,
            @RequestParam("resume") MultipartFile resumeFile,
            Model model, Principal principal) {

        try {
            addDefaultAttributes(model, principal);
            if (principal == null)
                return "redirect:/login?msg=Please sign in first";

            model.addAttribute("role", role);
            model.addAttribute("location", location);
            model.addAttribute("datePosted", (datePosted != null) ? datePosted : "all");

            if (role == null || role.isBlank())
                return "dashboard";

            String resumeText = "";
            if (resumeFile != null && !resumeFile.isEmpty()) {
                try {
                    resumeText = extractTextFromPDF(resumeFile);
                    model.addAttribute("resumeActive", true);
                    model.addAttribute("resumeFileName", resumeFile.getOriginalFilename());

                } catch (Exception e) {
                    model.addAttribute("error", "Resume Parsing: " + e.getMessage());
                }
            }

            // TRY REAL SEARCH -> IF FAIL, USE MOCK DATA
            try {
                // Note: The original fetchRealJobs method expects 'page' and 'experience' which
                // are no longer in search signature.
                // Assuming 'page' defaults to 1 and 'experience' is not used in fetchRealJobs
                // based on the diff.
                fetchRealJobs(model, role, location, 1, datePosted, resumeText);
            } catch (Exception e) {
                // Fallback to Mock Data if API fails
                System.out.println("API Error: " + e.getMessage() + ". Generating mock jobs.");
                generateMockJobs(model, role, location, resumeText, datePosted);
                model.addAttribute("error", "Network busy. Showing simulated results.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "System Reset: " + e.getMessage());
        }
        return "dashboard";
    }

    @SuppressWarnings("unchecked")
    private void fetchRealJobs(Model model, String role, String loc, int page, String date, String resumeText)
            throws Exception {
        String q = role.trim() + " in " + (loc != null ? loc.trim() : "Remote");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://jsearch.p.rapidapi.com/search")
                .queryParam("query", q)
                .queryParam("page", page)
                .queryParam("num_pages", 1);

        if (date != null && !date.equals("all")) {
            builder.queryParam("date_posted", date);
        }

        URI uri = builder.build().encode().toUri();

        HttpHeaders h = new HttpHeaders();
        h.set("X-RapidAPI-Key", rapidApiKey);
        h.set("X-RapidAPI-Host", "jsearch.p.rapidapi.com");

        ResponseEntity<Map> resp = new RestTemplate().exchange(uri, HttpMethod.GET, new HttpEntity<>(h), Map.class);
        if (resp.getBody() != null && resp.getBody().get("data") instanceof List) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) resp.getBody().get("data");
            List<Map<String, Object>> jobs = new ArrayList<>();
            for (Map<String, Object> r : raw) {
                Map<String, Object> j = new HashMap<>();
                String comp = Objects.toString(r.get("employer_name"), "Top Tech Corp");
                if (comp.trim().isEmpty())
                    comp = "Hiring Agency";

                j.put("title", Objects.toString(r.get("job_title"), "Professional Role"));
                j.put("company", comp);
                j.put("job_city", Objects.toString(r.get("job_city"), "Remote"));
                String d = Objects.toString(r.get("job_description"), "");
                j.put("desc", d.length() > 240 ? d.substring(0, 240) + "..." : d);
                j.put("url", Objects.toString(r.get("job_apply_link"), "#"));
                j.put("logo", (r.get("employer_logo") != null) ? r.get("employer_logo").toString()
                        : "https://via.placeholder.com/64?text=" + comp.substring(0, 1).toUpperCase());

                int score = calculateScore(resumeText, j.get("title").toString(), d);
                j.put("score", score);
                j.put("reason", score > 60 ? "Great technical match for your career." : "Location/Role match found.");
                jobs.add(j);
            }
            if (!resumeText.isEmpty())
                jobs.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
            model.addAttribute("jobs", jobs);
        }
    }

    private void generateMockJobs(Model model, String role, String loc, String resumeText, String datePosted) {
        List<Map<String, Object>> jobs = new ArrayList<>();
        String timeFrame = (datePosted != null) ? " (" + datePosted + ")" : "";
        String[] comps = { "Starlink", "OpenAI", "Nvidia", "Adobe", "Salesforce" };
        for (String c : comps) {
            Map<String, Object> j = new HashMap<>();
            j.put("title", role + " (Simulated)" + timeFrame);
            j.put("company", c);
            j.put("job_city", loc != null && !loc.isBlank() ? loc : "Global Remote");
            j.put("desc",
                    "This is an AI-generated match for your search. The live job network is currently under high load, but we can still analyze your profile against this criteria.");
            j.put("url", "https://google.com/search?q=" + role + "+jobs");
            j.put("logo", "https://via.placeholder.com/64?text=" + c.charAt(0));
            int score = calculateScore(resumeText, role, "");
            j.put("score", score);
            j.put("reason", "Simulated match for professional benchmarking.");
            jobs.add(j);
        }
        model.addAttribute("jobs", jobs);
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private int calculateScore(String res, String tit, String desc) {
        if (res == null || res.isEmpty())
            return 40;
        int s = 45;
        String[] kws = { "java", "node", "python", "react", "aws", "docker", "sql" };
        for (String k : kws)
            if (res.toLowerCase().contains(k))
                s += 10;
        return Math.min(s, 100);
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("Authorization", "Bearer " + groqApiKey);
            Map<String, Object> r = Map.of("model", "llama-3.3-70b-versatile", "messages",
                    List.of(Map.of("role", "user", "content", body.get("message"))));
            ResponseEntity<Map> rs = new RestTemplate().postForEntity("https://api.groq.com/openai/v1/chat/completions",
                    new HttpEntity<>(r, h), Map.class);
            return Map.of("reply",
                    (String) ((Map) ((Map) ((List) rs.getBody().get("choices")).get(0)).get("message")).get("content"));
        } catch (Exception e) {
            return Map.of("reply", "AI Busy.");
        }
    }
}
