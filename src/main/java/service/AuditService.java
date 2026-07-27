package service;

import model.AuditEvent;
import repository.DataRepository;
import repository.FileDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditService {
    private static final String FILE_AUDIT = "audit-log.dat";
    private final DataRepository<List<AuditEvent>> auditRepository;
    private List<AuditEvent> events = new ArrayList<>();

    public AuditService() {
        this(new FileDataRepository<>(FILE_AUDIT));
    }

    public AuditService(String storageFile) {
        this(new FileDataRepository<>(storageFile));
    }

    public AuditService(DataRepository<List<AuditEvent>> auditRepository) {
        if (auditRepository == null) {
            throw new IllegalArgumentException("Il repository audit è obbligatorio.");
        }
        this.auditRepository = auditRepository;
        loadEvents();
    }

    public void record(String actor, String role, String action, String target, String details) {
        events.add(new AuditEvent(
                LocalDateTime.now(),
                sanitize(actor),
                sanitize(role),
                sanitize(action),
                sanitize(target),
                sanitize(details)
        ));
        saveEvents();
    }

    public List<AuditEvent> getEvents() {
        List<AuditEvent> reversedEvents = new ArrayList<>(events);
        Collections.reverse(reversedEvents);
        return reversedEvents;
    }

    private void saveEvents() {
        auditRepository.save(events);
    }

    private void loadEvents() {
        List<AuditEvent> savedEvents = auditRepository.load();
        if (savedEvents != null) {
            events = savedEvents;
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

}
