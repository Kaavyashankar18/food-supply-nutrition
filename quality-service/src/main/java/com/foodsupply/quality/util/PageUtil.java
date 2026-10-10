package com.foodsupply.quality.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageUtil {

    private PageUtil() {
    }

    /**
     * Builds a Pageable. Only whitelisted sort fields are honoured; otherwise the default
     * field and direction are used. Page is clamped to >= 0 and size to 1..1000.
     */
    public static Pageable build(int page, int size, String sort, String dir, Set<String> allowedFields,
                                 String defaultField, Sort.Direction defaultDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 1000);

        boolean customSort = sort != null && allowedFields.contains(sort);
        String field = customSort ? sort : defaultField;
        Sort.Direction direction = customSort
                ? ("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC)
                : defaultDirection;

        Sort sorting = Sort.by(direction, field);
        if (!"id".equals(field)) {
            sorting = sorting.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return PageRequest.of(safePage, safeSize, sorting);
    }
}
