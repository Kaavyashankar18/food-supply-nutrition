package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProgrammeClient extends ApiClient {

    public ProgrammeClient(@Value("${services.programme.url}") String baseUrl) {
        super("programme-service", baseUrl);
    }
}
