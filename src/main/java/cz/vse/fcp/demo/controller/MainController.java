package cz.vse.fcp.demo.controller;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
public class MainController {

    @GetMapping("/")
    public Map index() {
        Map<String, Object> results = new HashMap<>();
        results.put("success", true);
        results.put("msg", "Welcome to the FCP API");
        return results;
    }

    @GetMapping("/fcp")
    public Map fcp(@RequestParam(required = false) List<String> iris) {
        Map<String, Object> results = new HashMap<>();
        if (null == iris) {
            results.put("success", false);
            results.put("msg", "Missing required param 'iris'");
            return results;
        }
        FCP fcp = new FCP();
        try {
            for (String iri : iris) {
                if (results.containsKey(iri)) {
                    continue;
                }
                results.put(iri, fcp.getCategorizationOptions(iri, true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

}