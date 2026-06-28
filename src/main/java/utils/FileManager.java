package utils;

import java.io.*;

public class FileManager {

    public static <T> void salvaSuFile(String filePath, T data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(data);
        } catch (IOException e) {
            throw new IllegalStateException("Errore durante il salvataggio su file: " + filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T caricaDaFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Errore durante il caricamento da file: " + filePath, e);
        }
    }
}
