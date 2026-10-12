package repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileDataRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void salvaECaricaDatiDaFile() {
        DataRepository<List<String>> repository = new FileDataRepository<>(tempDir.resolve("dati.dat").toString());

        repository.save(List.of("uno", "due"));

        assertEquals(List.of("uno", "due"), repository.load());
    }

    @Test
    void rifiutaPercorsoVuoto() {
        assertThrows(IllegalArgumentException.class, () -> new FileDataRepository<>(" "));
    }
}
