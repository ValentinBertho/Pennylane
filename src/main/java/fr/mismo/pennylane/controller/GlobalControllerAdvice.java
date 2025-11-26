package fr.mismo.pennylane.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("httpServletRequest")
    public HttpServletRequest getHttpServletRequest(HttpServletRequest request) {
        return request;
    }
}

