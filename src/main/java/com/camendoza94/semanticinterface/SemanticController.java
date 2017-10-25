package com.camendoza94.semanticinterface;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@RequestMapping("/interface")
class SemanticController {

    private final HashMap<String, Method> cache = new HashMap<>();

    private final RestTemplate template = new RestTemplate();

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<String> requestService(@RequestBody DeviceObservation observation) {
        String deviceID = observation.getDeviceId();
        String URL;
        ArrayList<String> serviceFields;
        ArrayList<String> serviceTypes;
        JsonParser parser = new JsonParser();
        JsonObject body = parser.parse(observation.getPayload()).getAsJsonObject();
        if (cache.get(deviceID) == null) {
            HashMap<String, String> matches = getMatchingFromCSV();
            System.out.println("Device ID: " + deviceID);
            String label = getDeviceValue(deviceID);
            if (StringUtils.isEmpty(label)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Device not found");
            }
            String[] service = getServicePostAPI(matches.get(label));
            System.out.println("Service: " + Arrays.toString(service));
            if (service[0] == null || service[1] == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Service not found");
            }
            URL = "http://" + service[0] + "/" + service[1];
            String[] fields = service[2].split(" ");
            String[] fieldTypes = service[3].split(" ");
            serviceFields = new ArrayList<>(Arrays.asList(fields));
            serviceTypes = new ArrayList<>(Arrays.asList(fieldTypes));
            cache.put(deviceID, new Method(URL, serviceFields, serviceTypes));
        } else {
            URL = cache.get(deviceID).getURL();
            serviceFields = cache.get(deviceID).getFields();
            serviceTypes = cache.get(deviceID).getTypes();
        }
        JsonObject requestJson = new JsonObject();
        for (int i = 0; i < serviceFields.size(); i++) {
            String serviceField = serviceFields.get(i);
            try {
                if (serviceTypes.get(i).equalsIgnoreCase("String"))
                    requestJson.addProperty(serviceField, body.get(serviceField).getAsString());
                else
                    requestJson.addProperty(serviceField, body.get(serviceField).getAsNumber());
            } catch (NullPointerException e) {
                System.out.println("Field " + serviceField + " not found. Skipping.");
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);
        return template.postForEntity(URL, entity, String.class);
    }

    private static String[] getServicePostAPI(String matched) {
        String[] info = new String[4];
        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/services#>\n" +
                "SELECT ?URI ?methodValue (group_concat(?bodyLabel) as ?bodyLabels) (group_concat(?dataTypeLabel) as ?dataTypes)\n" +
                "WHERE {\n" +
                " ?service a :Service; :aboutProperty ?property .\n" +
                " ?service :hasAPIURL ?URL; :hasMethod ?method .\n" +
                " ?URL :hasStringValue ?URI .\n" +
                " ?method a :Create; :hasStringValue ?methodValue; :hasBodyField ?bodyField .\n" +
                " ?bodyField rdfs:label ?bodyLabel; :hasDataType ?dataType .\n" +
                " ?dataType rdfs:label ?dataTypeLabel .\n" +
                "FILTER (?property = <" + matched + ">)\n" +
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

//    private static String[] getServicePostAPI(String label, String matched) {
//        String[] info = new String[4];
//        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/services#>\n" +
//                "SELECT ?URI ?methodValue (group_concat(?bodyLabel) as ?bodyLabels) (group_concat(?dataTypeLabel) as ?dataTypes)\n" +
//                "WHERE {\n" +
//                " ?service a :Service; (:|!:)* ?property .\n" +
//                " ?property a <" + matched + ">; rdfs:label '" + label + "' .\n" +
//                " ?service :hasAPIURL ?URL; :hasMethod ?method .\n" +
//                " ?URL :hasStringValue ?URI .\n" +
//                " ?method a :Create; :hasStringValue ?methodValue; :hasBodyField ?bodyField .\n" +
//                " ?bodyField rdfs:label ?bodyLabel; :hasDataType ?dataType .\n" +
//                " ?dataType rdfs:label ?dataTypeLabel .\n" +
//                "}\n" +
//                "GROUP BY ?URI ?methodValue";
//        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/virtual/query", QueryFactory.create(qs));
//
//
//        ResultSet results = exec.execSelect();
//
//        if (results.hasNext()) {
//            QuerySolution next = results.next();
//            info[0] = next.get("URI").toString();
//            info[1] = next.get("methodValue").toString();
//            info[2] = next.get("bodyLabels").toString();
//            info[3] = next.get("dataTypes").toString();
//        }
//        return info;
//    }

    private static String getDeviceValue(String deviceId) {
        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/iotdevices#>\n" +
                "SELECT ?property\n" +
                "WHERE {\n" +
                " ?device a :Sensor; :hasDeviceId '" + deviceId + "'; (:|!:)* ?property .\n" +
                " ?property a :Property .\n" +
                "}";
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/fisica/query", QueryFactory.create(qs));

        ResultSet results = exec.execSelect();

        if (results.hasNext()) {
            QuerySolution next = results.next();
            return next.get("property").toString();
        }
        return "";
    }

//    private static String getMatchLabel(String matched, String deviceId) {
//        System.out.println("Matched Class: " + matched);
//        String qs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//                "PREFIX : <http://www.semanticweb.org/ca.mendoza968/ontologies/iotdevices#>\n" +
//                "SELECT ?label\n" +
//                "WHERE {\n" +
//                " ?device a :Sensor; :hasDeviceId '" + deviceId + "'; (:|!:)* ?u .\n" +
//                " ?u a <" + matched + ">; rdfs:label ?label\n" +
//                "}";
//        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/fisica/query", QueryFactory.create(qs));
//
//        ResultSet results = exec.execSelect();
//
//        if (results.hasNext()) {
//            QuerySolution next = results.next();
//            return next.get("label").toString();
//        }
//        return "";
//    }

    private static HashMap<String, String> getMatchingFromCSV() {
        HashMap<String, String> matches = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src/data/link.csv"))) {
            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                String iotClass = line.split(",")[1].trim();
                String serviceClass = line.split(",")[2].trim();
                matches.put(iotClass, serviceClass);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matches;
    }

//    private static List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> queryMatching() {
//        List<AbstractMap.SimpleEntry<RDFNode, RDFNode>> matches = new ArrayList<>();
//        String qs = "PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
//                "SELECT ?entity1 ?entity2\n" +
//                "WHERE {\n" +
//                "  ?subject align:entity1 ?entity1 .\n" +
//                "  ?subject align:entity2 ?entity2\n" +
//                "}";
//        QueryExecution exec = QueryExecutionFactory.sparqlService("http://localhost:3030/matching/query", QueryFactory.create(qs));
//
//        ResultSet results = exec.execSelect();
//
//        while (results.hasNext()) {
//            QuerySolution next = results.next();
//            matches.add(new AbstractMap.SimpleEntry<>(next.get("entity2"), next.get("entity1")));
//        }
//
//        return matches;
//    }
}