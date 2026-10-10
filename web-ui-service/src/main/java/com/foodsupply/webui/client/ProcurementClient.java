package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProcurementClient extends ApiClient {

    public ProcurementClient(@Value("${services.procurement.url}") String baseUrl) {
        super("procurement-service", baseUrl);
    }
}
