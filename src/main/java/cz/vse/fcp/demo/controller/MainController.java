package cz.vse.fcp.demo.controller;


import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@RestController
public class MainController {
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.SECONDS).noTransform().mustRevalidate();

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> results = new HashMap<>();
        results.put("success", true);
        results.put("msg", "Welcome to the FCP API");
        return ResponseEntity.ok().cacheControl(cacheControl).body(results);
    }

    @GetMapping("/fcp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fcp(@RequestParam String iri, @RequestParam(required = false) List<String> selectedClasses) {
        Map<String, Object> results = new HashMap<>();
        FCP fcp = new FCP();
        try {
            if (iri.trim().length() == 0) {
                throw new Exception("Invalid value of request param 'iri'");
            }
            results.put(iri, fcp.getCategorizationOptions(iri, true, selectedClasses == null ? new ArrayList<>() :  selectedClasses));
            results.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            results.put("msg", e.getMessage());
            results.put("success", false);
            return ResponseEntity.status(500).body(results);
        }
        return ResponseEntity.ok().cacheControl(cacheControl).body(results);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        Map<String, Object> results = new HashMap<>();
        results.put("msg", name + " parameter is missing");
        results.put("success", false);
        return ResponseEntity.badRequest().cacheControl(cacheControl).body(results);
    }
}