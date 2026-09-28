package service;

import factory.CategoriaProdotto;
import factory.ProdottoFactory;
import factory.ProdottoFactoryProvider;
import model.Prodotto;
import repository.DataRepository;
import repository.FileDataRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductService {
    private static final String FILE_PRODOTTI = "prodotti.dat";
    private static final String PLACEHOLDER_METADATA = "Da definire";
    private final DataRepository<List<Prodotto>> productRepository;
    private List<Prodotto> prodotti = new ArrayList<>();

    public ProductService() {
        this(new FileDataRepository<>(FILE_PRODOTTI));
    }

    public ProductService(String storageFile) {
        this(createRepository(storageFile));
    }

    public ProductService(DataRepository<List<Prodotto>> productRepository) {
        if (productRepository == null) {
            throw new IllegalArgumentException("Il repository prodotti è obbligatorio.");
        }
        this.productRepository = productRepository;
        caricaProdotti();
    }

    private static DataRepository<List<Prodotto>> createRepository(String storageFile) {
        if (isBlank(storageFile)) {
            throw new IllegalArgumentException("Il file di persistenza dei prodotti non può essere vuoto.");
        }
        return new FileDataRepository<>(storageFile);
    }

    public Prodotto creaEInserisciProdotto(
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto,
            CategoriaProdotto categoria
    ) {
        validaProdotto(codice, nome, descrizione, brand, tipoProdotto, quantita, costo, sconto, categoria);
        validaCodiceDisponibile(codice, null);

        ProdottoFactory factory = ProdottoFactoryProvider.getFactory(categoria);
        Prodotto prodotto = factory.creaProdotto(
                sanitize(codice),
                sanitize(nome),
                sanitize(descrizione),
                sanitize(brand),
                sanitize(tipoProdotto),
                optional(utilizzo),
                quantita,
                costo,
                sconto
        );
        prodotti.add(prodotto);
        salvaProdotti();
        return prodotto;
    }

    public Prodotto aggiornaProdotto(
            String codiceOriginale,
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto,
            CategoriaProdotto categoria
    ) {
        validaProdotto(codice, nome, descrizione, brand, tipoProdotto, quantita, costo, sconto, categoria);
        int index = trovaIndiceProdotto(codiceOriginale);
        validaCodiceDisponibile(codice, codiceOriginale);

        ProdottoFactory factory = ProdottoFactoryProvider.getFactory(categoria);
        Prodotto prodottoAggiornato = factory.creaProdotto(
                sanitize(codice),
                sanitize(nome),
                sanitize(descrizione),
                sanitize(brand),
                sanitize(tipoProdotto),
                optional(utilizzo),
                quantita,
                costo,
                sconto
        );
        prodotti.set(index, prodottoAggiornato);
        salvaProdotti();
        return prodottoAggiornato;
    }

    public void eliminaProdotto(String codice) {
        int index = trovaIndiceProdotto(codice);
        prodotti.remove(index);
        salvaProdotti();
    }

    public void eliminaProdotti(List<Prodotto> prodottiDaEliminare) {
        if (prodottiDaEliminare == null || prodottiDaEliminare.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un prodotto da eliminare.");
        }

        Set<String> codiciDaEliminare = prodottiDaEliminare.stream()
                .map(Prodotto::getCodice)
                .collect(Collectors.toSet());
        prodotti.removeIf(prodotto -> codiciDaEliminare.contains(prodotto.getCodice()));
        salvaProdotti();
    }

    public Prodotto aggiornaQuantita(String codice, int nuovaQuantita) {
        if (nuovaQuantita < 0) {
            throw new IllegalArgumentException("La quantità non può essere negativa.");
        }

        Prodotto prodotto = getProdottoByCodice(codice);
        return aggiornaProdotto(
                prodotto.getCodice(),
                prodotto.getCodice(),
                prodotto.getNome(),
                prodotto.getDescrizione(),
                prodotto.getBrand(),
                prodotto.getTipoProdotto(),
                prodotto.getUtilizzo(),
                nuovaQuantita,
                prodotto.getCosto(),
                prodotto.getSconto(),
                CategoriaProdotto.fromLabel(prodotto.getCategoria())
        );
    }

    public Prodotto getProdottoByCodice(String codice) {
        for (Prodotto prodotto : prodotti) {
            if (prodotto.getCodice().equalsIgnoreCase(codice)) {
                return prodotto;
            }
        }
        throw new IllegalArgumentException("Prodotto non trovato.");
    }

    public List<Prodotto> getProdotti() {
        return new ArrayList<>(prodotti);
    }

    private void salvaProdotti() {
        productRepository.save(prodotti);
    }

    private void caricaProdotti() {
        List<Prodotto> prodottiSalvati = productRepository.load();
        if (prodottiSalvati != null) {
            prodotti = normalizzaProdottiLegacy(prodottiSalvati);
            if (!prodotti.equals(prodottiSalvati)) {
                salvaProdotti();
            }
        }
    }

    private List<Prodotto> normalizzaProdottiLegacy(List<Prodotto> prodottiSalvati) {
        List<Prodotto> prodottiNormalizzati = new ArrayList<>();
        for (Prodotto prodotto : prodottiSalvati) {
            if (isBlank(prodotto.getBrand()) || isBlank(prodotto.getTipoProdotto())) {
                CategoriaProdotto categoria = CategoriaProdotto.fromLabel(prodotto.getCategoria());
                ProdottoFactory factory = ProdottoFactoryProvider.getFactory(categoria);
                prodottiNormalizzati.add(factory.creaProdotto(
                        prodotto.getCodice(),
                        prodotto.getNome(),
                        prodotto.getDescrizione(),
                        isBlank(prodotto.getBrand()) ? PLACEHOLDER_METADATA : prodotto.getBrand(),
                        isBlank(prodotto.getTipoProdotto()) ? PLACEHOLDER_METADATA : prodotto.getTipoProdotto(),
                        prodotto.getUtilizzo(),
                        prodotto.getQuantita(),
                        prodotto.getCosto(),
                        prodotto.getSconto()
                ));
            } else {
                prodottiNormalizzati.add(prodotto);
            }
        }
        return prodottiNormalizzati;
    }

    private void validaProdotto(
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            int quantita,
            double costo,
            double sconto,
            CategoriaProdotto categoria
    ) {
        if (isBlank(codice) || isBlank(nome) || isBlank(descrizione) || isBlank(brand) || isBlank(tipoProdotto)) {
            throw new IllegalArgumentException("Compila codice, nome, descrizione, brand e tipo prodotto.");
        }

        if (categoria == null) {
            throw new IllegalArgumentException("Seleziona una categoria valida.");
        }

        if (quantita < 0) {
            throw new IllegalArgumentException("La quantità non può essere negativa.");
        }

        if (costo < 0) {
            throw new IllegalArgumentException("Il costo non può essere negativo.");
        }

        if (sconto < 0 || sconto > 100) {
            throw new IllegalArgumentException("Lo sconto deve essere compreso tra 0 e 100.");
        }
    }

    private void validaCodiceDisponibile(String codice, String codiceDaIgnorare) {
        boolean codiceGiaPresente = prodotti.stream()
                .anyMatch(prodotto -> prodotto.getCodice().equalsIgnoreCase(codice)
                        && (codiceDaIgnorare == null || !prodotto.getCodice().equalsIgnoreCase(codiceDaIgnorare)));

        if (codiceGiaPresente) {
            throw new IllegalArgumentException("Esiste gia un prodotto con questo codice.");
        }
    }

    private int trovaIndiceProdotto(String codice) {
        for (int i = 0; i < prodotti.size(); i++) {
            if (prodotti.get(i).getCodice().equalsIgnoreCase(codice)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Prodotto non trovato.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String sanitize(String value) {
        return value.trim();
    }

    private String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
