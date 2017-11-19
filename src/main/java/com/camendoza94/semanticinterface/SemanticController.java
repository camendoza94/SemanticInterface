package com.camendoza94.semanticinterface;

import com.camendoza94.exceptions.NoMatchesFoundException;
import com.camendoza94.exceptions.ServiceNotFoundInOntologyException;
import com.camendoza94.matching.DukeMatching;
import org.apache.jena.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/interface")
class SemanticController {

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<String> requestService(@RequestBody String deviceID) throws NoMatchesFoundException, ServiceNotFoundInOntologyException {
        HashMap<String, ArrayList<String>> matches = getMatchingFromCSV();
        System.out.println("Device ID: " + deviceID);
        ArrayList<String[]> services = new ArrayList<>();
        //Device is not in ontology
        if (matches.get(deviceID) == null) {
            throw new NoMatchesFoundException();
        }
        for (String match : matches.get(deviceID)) {
            services.add(getServicePostAPI(match));
        }
        //Matched service is not in ontology
        if (services.isEmpty()) {
            throw new ServiceNotFoundInOntologyException();
        }
        StringBuilder URL = new StringBuilder();
        for (String[] service : services) {
            URL.append(service[0]).append("/").append(service[1]).append("\n");
        }
        return ResponseEntity.ok().body(URL.toString());
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

    private static HashMap<String, ArrayList<String>> getMatchingFromCSV() throws NoMatchesFoundException {
        HashMap<String, ArrayList<String>> matches = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src/data/link.csv"))) {
            String line = br.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                if (parts.length != 4)
                    throw new FileNotFoundException(); //TODO Change exception
                String deviceId = line.split(",")[1].trim();
                String methodInstance = line.split(",")[2].trim();
                matches.computeIfAbsent(deviceId, k -> new ArrayList<>());
                matches.get(deviceId).add(methodInstance);
                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            try {
                DukeMatching.matching();
                getMatchingFromCSV();
            } catch (IOException | SAXException e1) {
                throw new NoMatchesFoundException();
            }
        } catch (IOException e) {
            throw new NoMatchesFoundException();
        }
        return matches;
    }

    @ExceptionHandler
    void handleDeviceNotFoundException(NoMatchesFoundException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.PRECONDITION_FAILED.value());
    }

    @ExceptionHandler
    void handleServiceNotFoundException(ServiceNotFoundInOntologyException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}