package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient extends ApiClient {

    public InventoryClient(@Value("${services.inventory.url}") String baseUrl) {
        super("inventory-service", baseUrl);
    }
}
