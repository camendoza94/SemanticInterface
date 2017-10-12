package com.camendoza94.semanticinterface;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/interface")
class SemanticController {

    private HashMap<String, String> cache = new HashMap<>();

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<String> requestService(@RequestBody DeviceObservation observation) {
        String deviceID = observation.getDeviceId();
        String URL;
        RestTemplate template = new RestTemplate();
        System.out.println(cache.get(deviceID));
        if(cache.get(deviceID) == null ) {
            List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> matches = queryMatching();
            System.out.println("Device ID: " + deviceID);
            String label = getMatchLabel(matches.get(0).getKey().toString(), deviceID);
            if (StringUtils.isEmpty(label)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Device not found");
            }
            System.out.println("Label: " + label);
            String[] service = getServiceAPI(label, matches.get(0).getValue().toString());
            System.out.println("Service: " + Arrays.toString(service));
            if (service[0] == null || service[1] == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Service not found");
            }
            Measurement measurement = new Measurement();
            measurement.setTimestamp(observation.getTimestamp());
            measurement.setValue(observation.getValue());
            URL = "http://" + service[0] + "/" + service[1];
            System.out.println(URL);
            cache.put(deviceID, URL);
        } else {
            URL = cache.get(deviceID);
        }
        return template.postForEntity(URL, observation, String.class);
    }

    private static String[] getServiceAPI(String label, String matched) {
        String[] info = new String[2];
        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/services#>\n" +
                "SELECT ?URI ?methodValue\n" +
                "WHERE {\n" +
                " ?service a :Service; (:|!:)* ?property .\n" +
                " ?property a <" + matched + ">; rdfs:label '" + label + "'.\n" +
                " ?service :hasAPIURL ?URL; :hasMethod ?method .\n" +
                " ?URL :hasStringValue ?URI.\n" +
                " ?method a :Create; :hasStringValue ?methodValue .\n" +
                "}";
        System.out.println(qs);
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/virtual/sparql", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();
        if (results.hasNext()) {
            QuerySolution next = results.next();
            info[0] = next.get("URI").toString();
            info[1] = next.get("methodValue").toString();
        }
        return info;
    }

    private static String getMatchLabel(String matched, String deviceId) {
        //System.out.println("Matched Class: " + matched);
        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/iotdevices#>\n" +
                "SELECT ?label\n" +
                "WHERE {\n" +
                " ?device a :Sensor; :hasDeviceId '" + deviceId + "'; (:|!:)* ?property .\n" +
                " ?property a <" + matched + ">; rdfs:label ?label\n" +
                "}";
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/fisica/query", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();

        if (results.hasNext()) {
            QuerySolution next = results.next();
            return next.get("label").toString();
        }
        return "";
    }

    private static List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> queryMatching() {
        List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> matches = new ArrayList<>();
        String qs = "PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
                "SELECT ?entity1 ?entity2\n" +
                "WHERE {\n" +
                "  ?subject align:entity1 ?entity1 .\n" +
                "  ?subject align:entity2 ?entity2\n" +
                "}";
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/matching/query", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();

        while (results.hasNext()) {
            QuerySolution next = results.next();
            matches.add(new AbstractMap.SimpleEntry<>(next.get("entity2"), next.get("entity1")));
        }

        return matches;
    }
}