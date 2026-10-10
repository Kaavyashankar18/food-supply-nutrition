package com.foodsupply.procurement.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageUtil {

    private PageUtil() {
    }

    /** Builds a Pageable; the sort field is whitelisted, size is clamped to 1..1000. */
    public static Pageable of(int page, int size, String sort, String dir, Set<String> allowedSortFields,
                              String defaultSort, Sort.Direction defaultDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 1000);
        boolean validSort = sort != null && allowedSortFields.contains(sort);
        String field = validSort ? sort : defaultSort;
        Sort.Direction direction = validSort ? Sort.Direction.ASC : defaultDir;
        if ("asc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.DESC;
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
