package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DistributionClient extends ApiClient {

    public DistributionClient(@Value("${services.distribution.url}") String baseUrl) {
        super("distribution-service", baseUrl);
    }
}
