package utils;

import java.io.*;

/**
 * Classe di utilità per la gestione delle operazioni di salvataggio e caricamento dei dati su file.
 * Utilizza la serializzazione per memorizzare e recuperare oggetti in modo persistente.
 */
public class FileManager {

    /**
     * Salva un oggetto su file utilizzando la serializzazione.
     *
     * @param filePath Il percorso del file in cui salvare i dati
     * @param data     L'oggetto da salvare (deve implementare {@link Serializable})
     * @param <T>      Il tipo generico dell'oggetto da salvare
     */
    public static <T> void salvaSuFile(String filePath, T data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(data); // Serializza l'oggetto e lo scrive nel file
        } catch (IOException e) {
            e.printStackTrace(); // Gestione dell'errore in caso di problemi di I/O
        }
    }

    /**
     * Carica un oggetto da un file utilizzando la deserializzazione.
     *
     * @param filePath Il percorso del file da cui caricare i dati
     * @param <T>      Il tipo generico dell'oggetto da caricare
     * @return         L'oggetto deserializzato dal file, o {@code null} se si verifica un errore
     */
    public static <T> T caricaDaFile(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (T) ois.readObject(); // Legge e deserializza l'oggetto dal file
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("⚠️ Nessun file trovato o errore nel caricamento: " + filePath);
            return null; // Restituisce null in caso di errore (file mancante o incompatibilità)
        }
    }
}

