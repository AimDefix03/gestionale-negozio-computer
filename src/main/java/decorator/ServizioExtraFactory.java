package decorator;

import model.Prodotto;

public final class ServizioExtraFactory {
    private static final String NESSUNA_OPZIONE = "Nessuna";
    private static final String GARANZIA_ESTESA = "Garanzia Estesa";
    private static final String ASSEMBLAGGIO_PC = "Assemblaggio PC";
    private static final String PACCHETTO_MANUTENZIONE = "Pacchetto Manutenzione";

    private ServizioExtraFactory() {
    }

    public static String[] getOpzioni() {
        return new String[]{NESSUNA_OPZIONE, GARANZIA_ESTESA, ASSEMBLAGGIO_PC, PACCHETTO_MANUTENZIONE};
    }

    public static Prodotto applica(String servizio, Prodotto prodotto) {
        if (servizio == null || NESSUNA_OPZIONE.equals(servizio)) {
            return prodotto;
        }

        return switch (servizio) {
            case GARANZIA_ESTESA -> new GaranziaEstesa(prodotto);
            case ASSEMBLAGGIO_PC -> new AssemblaggioPC(prodotto);
            case PACCHETTO_MANUTENZIONE -> new PacchettoManutenzione(prodotto);
            default -> throw new IllegalArgumentException("Servizio extra non supportato: " + servizio);
        };
    }
}
