package com.career.agent.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model, Principal principal) {
        // Collect error info for logs
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object exception = request.getAttribute("jakarta.servlet.error.exception");

        System.err.println("CRITICAL ERROR CAUGHT: Status=" + status + " Exception=" + exception);
        if (exception instanceof Exception) {
            ((Exception) exception).printStackTrace();
        }

        // Return clean dashboard attributes to prevent rendering crashes
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("userEmail", (principal != null) ? principal.getName() : "");
        model.addAttribute("jobs", new ArrayList<>());
        model.addAttribute("currentPage", 1);
        model.addAttribute("role", "");
        model.addAttribute("location", "");
        model.addAttribute("experience", "all");
        model.addAttribute("datePosted", "all");
        model.addAttribute("resumeActive", false);
        model.addAttribute("resumeFileName", "");

        model.addAttribute("error",
                "The service encountered a temporary issue (Code: " + status + "). Please try refreshing your search.");
        return "dashboard";
    }
}
