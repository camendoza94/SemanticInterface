package com.camendoza94.semanticinterface;

import com.camendoza94.matching.DukeMatching;
import org.apache.jena.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@RequestMapping("/interface")
class SemanticController {

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<String> requestService(@RequestBody String deviceID) {
        HashMap<String, String> matches = getMatchingFromCSV();
        System.out.println("Device ID: " + deviceID);
        String[] service = getServicePostAPI(matches.get(deviceID));
        System.out.println("Service: " + Arrays.toString(service));
        if (service[0] == null || service[1] == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Service not found");
        }
        String URL =  service[0] + "/" + service[1];
        return ResponseEntity.status(HttpStatus.OK).body(URL);
    }

    private static String[] getServicePostAPI(String matched) {
        String[] info = new String[4];
        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/services#>\n" +
                "SELECT ?URI ?methodValue (group_concat(?bodyLabel) as ?bodyLabels) (group_concat(?dataTypeLabel) as ?dataTypes)\n" +
                "WHERE {\n" +
                " ?service a :Service .\n" +
                " ?service :hasAPIURL ?URL; :hasMethod ?method .\n" +
                " ?URL :hasStringValue ?URI .\n" +
                " ?method a :Create; :hasStringValue ?methodValue; :hasBodyField ?bodyField .\n" +
                " ?bodyField rdfs:label ?bodyLabel; :hasDataType ?dataType .\n" +
                " ?dataType rdfs:label ?dataTypeLabel .\n" +
                " FILTER (?method = <" + matched + ">) .\n" +
                "}\n" +
                "GROUP BY ?URI ?methodValue";
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/virtual/query", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();

        if (results.hasNext()) {
            QuerySolution next = results.next();
            info[0] = next.get("URI").toString();
            info[1] = next.get("methodValue").toString();
            info[2] = next.get("bodyLabels").toString();
            info[3] = next.get("dataTypes").toString();
        }
        return info;
    }

    private static HashMap<String, String> getMatchingFromCSV() {
        HashMap<String, String> matches = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src/data/link.csv"))) {
            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                String deviceId = line.split(",")[1].trim();
                String methodClass = line.split(",")[2].trim();
                matches.put(deviceId, methodClass);
                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            try {
                DukeMatching.matching();
            } catch (IOException | SAXException e1) {
                System.out.println("Error while matching.");
                e1.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Couldn't find any matches.");
            e.printStackTrace();
        }
        return matches;
    }
}