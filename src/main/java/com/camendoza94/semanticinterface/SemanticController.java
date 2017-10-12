package com.camendoza94.semanticinterface;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/interface")
class SemanticController {

    @RequestMapping(method = RequestMethod.POST)
    void requestService(@RequestBody DeviceObservation observation) {
        String deviceID = observation.getDeviceId();
        List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> matches = queryMatching();
        getMatchLabel(matches.get(0).getKey().toString(), deviceID); //TODO revisar

        //TODO find service with given property
        //TODO make POST request to API URL of service with body on observation
        Measurement measurement = new Measurement();
        measurement.setTimestamp(observation.getTimestamp());
        measurement.setValue(observation.getValue());
        RestTemplate template = new RestTemplate();
        //template.postForEntity("http://localhost:8080/measurements", observation, String.class);
    }

    private static String getMatchLabel(String matched, String deviceId) {
        String qs = "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/iotdevices#>\n" +
                "SELECT ?label\n" +
                "WHERE {\n" +
                " ?device a :Sensor; :hasDeviceId '" + deviceId + "'; (:|!:)* ?property .\n" +
                " ?property a <" + matched + ">; rdfs:label ?label\n" +
                "}";
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/fisica/query", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();

        if (results.hasNext()) {
            QuerySolution next = results.next();
            return next.get("label").asLiteral().getString();
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
            matches.add(new AbstractMap.SimpleEntry<>(next.get("entity1"), next.get("entity2")));
        }

        return matches;
    }
}