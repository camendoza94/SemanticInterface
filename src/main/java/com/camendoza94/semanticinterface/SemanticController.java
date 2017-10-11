package com.camendoza94.semanticinterface;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/interface")
class SemanticController {

    @RequestMapping(method = RequestMethod.POST)
    void requestService(@RequestBody DeviceObservation observation) {
        String deviceID = observation.getDeviceId();
        //TODO find measurement property
        //TODO find service with measurement property
        //TODO make POST request to API URL of service with body on observation
        Measurement measurement = new Measurement();
        measurement.setTimestamp(observation.getTimestamp());
        measurement.setValue(observation.getValue());
        RestTemplate template = new RestTemplate();
        template.postForEntity("http://localhost:8080/measurements", observation, String.class);
    }
}