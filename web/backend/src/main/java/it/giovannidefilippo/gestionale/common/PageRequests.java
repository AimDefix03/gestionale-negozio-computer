package it.giovannidefilippo.gestionale.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {
    private static final int DEFAULT_SIZE = 25;
    private static final int MAX_SIZE = 500;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }
}
