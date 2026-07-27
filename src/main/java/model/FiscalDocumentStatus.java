package model;

import java.io.Serializable;

public enum FiscalDocumentStatus implements Serializable {
    DRAFT("Bozza"),
    ISSUED("Emesso"),
    RECTIFIED("Rettificato");

    private final String label;

    FiscalDocumentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLocked() {
        return this == ISSUED || this == RECTIFIED;
    }
}
