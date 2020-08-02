package com.example.demo.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    @RequestMapping("/fcp_api")
    public Map index() {
        //for OntoFarm
        String OntoFarmCollectionList = "117810";
        //String OntoFarmCollectionList = "117810";
        String ontologies[] = OntoFarmCollectionList.split(",");
        Map<String, Object> data = null;

        //System.exit(1);

        FCP fcp = new FCP();
        BufferedReader reader;
        List list = new ArrayList();
        try {

            for(String s : ontologies) {
                System.out.println(s);
                data = fcp.getCategorizationOptions("https://owl.vse.cz/ontologies/"+s.trim()+".owl", true);
            }

            //run for one ontology
            //String onto = "https://owl.vse.cz/ontologies/117810.owl"; //cmt.owl
            //fcp.getCategorizationOptions(onto, true);


            reader = new BufferedReader(new FileReader("a.txt"));
            String line = reader.readLine();

            while (line != null) {
                list.add(line);
                line = reader.readLine();
            }
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Map<String,Object> response=new HashMap<>();
        response.put("status", HttpStatus.CREATED.value());
        response.put("data", data);


        return response;

    }

}