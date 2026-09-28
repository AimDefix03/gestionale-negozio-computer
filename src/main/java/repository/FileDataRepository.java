package repository;

import utils.FileManager;

public class FileDataRepository<T> implements DataRepository<T> {
    private final String filePath;

    public FileDataRepository(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Il percorso del file non può essere vuoto.");
        }
        this.filePath = filePath;
    }

    @Override
    public T load() {
        return FileManager.caricaDaFile(filePath);
    }

    @Override
    public void save(T data) {
        FileManager.salvaSuFile(filePath, data);
    }
}
