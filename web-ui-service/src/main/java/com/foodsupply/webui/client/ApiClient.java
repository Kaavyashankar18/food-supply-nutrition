package com.foodsupply.webui.client;

import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/** Base class for the thin per-service clients. Everything is passed around as plain maps. */
public abstract class ApiClient {

    protected static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<Map<String, Object>>() { };
    protected static final ParameterizedTypeReference<List<Map<String, Object>>> LIST =
            new ParameterizedTypeReference<List<Map<String, Object>>>() { };

    protected final BackendClient api;

    protected ApiClient(String serviceName, String baseUrl) {
        this.api = new BackendClient(serviceName, baseUrl);
    }

    /** GET a paged list; nameValue is name1, value1, name2, value2 ... (blank values are skipped). */
    public Map<String, Object> page(String path, int page, int size, String... nameValue) {
        Query query = new Query(path);
        for (int i = 0; i + 1 < nameValue.length; i += 2) {
            query.add(nameValue[i], nameValue[i + 1]);
        }
        query.add("page", page).add("size", size);
        return api.get(query.uri(), MAP, query.vars());
    }

    public List<Map<String, Object>> list(String path, String... nameValue) {
        Query query = new Query(path);
        for (int i = 0; i + 1 < nameValue.length; i += 2) {
            query.add(nameValue[i], nameValue[i + 1]);
        }
        return api.get(query.uri(), LIST, query.vars());
    }

    /** GET something that is either a plain JSON array or a paged object with a "content" array. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> items(String uri, Object... vars) {
        Object body = api.get(uri, new ParameterizedTypeReference<Object>() { }, vars);
        if (body instanceof Map<?, ?> m && m.get("content") instanceof List<?> content) {
            return (List<Map<String, Object>>) content;
        }
        if (body instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return new java.util.ArrayList<>();
    }

    public Map<String, Object> get(String uri, Object... vars) {
        return api.get(uri, MAP, vars);
    }

    public Map<String, Object> post(String uri, Object body, Object... vars) {
        return api.post(uri, body, MAP, vars);
    }

    public Map<String, Object> put(String uri, Object body, Object... vars) {
        return api.put(uri, body, MAP, vars);
    }

    public Map<String, Object> patch(String uri, Object... vars) {
        return api.patch(uri, MAP, vars);
    }

    public void delete(String uri, Object... vars) {
        api.delete(uri, vars);
    }
}
