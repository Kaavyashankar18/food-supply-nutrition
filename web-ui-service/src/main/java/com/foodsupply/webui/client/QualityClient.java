package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QualityClient extends ApiClient {

    public QualityClient(@Value("${services.quality.url}") String baseUrl) {
        super("quality-service", baseUrl);
    }
}
