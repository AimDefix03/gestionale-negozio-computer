package it.giovannidefilippo.gestionale.system;

import it.giovannidefilippo.gestionale.common.ApiError;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class ApiErrorMonitor {
    private static final int MAX_ERRORS = 20;

    private final Deque<RecentApiError> recentErrors = new ArrayDeque<>();

    public synchronized void record(ApiError error) {
        if (error.status() < 500) {
            return;
        }
        recentErrors.addFirst(new RecentApiError(error.timestamp(), error.status(), error.code(), error.message(), error.path(), error.requestId()));
        while (recentErrors.size() > MAX_ERRORS) {
            recentErrors.removeLast();
        }
    }

    public synchronized List<RecentApiError> recentErrors() {
        return List.copyOf(new ArrayList<>(recentErrors));
    }
}
