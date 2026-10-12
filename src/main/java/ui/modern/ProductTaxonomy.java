package ui.modern;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ProductTaxonomy {
    static final String UNDEFINED = "Da definire";
    static final String OTHER = "Altro";
    static final String ALL_BRANDS = "Tutti i brand";
    static final String ALL_TYPES = "Tutti i tipi";
    static final String[] BRAND_OPTIONS = {
            UNDEFINED,
            "NVIDIA",
            "AMD",
            "Intel",
            "Samsung",
            "Corsair",
            "Logitech",
            "Microsoft",
            "Adobe",
            "JetBrains",
            "ASUS",
            "MSI",
            "Gigabyte",
            "LG",
            "HP",
            OTHER
    };
    static final String[] PRODUCT_TYPE_OPTIONS = {
            UNDEFINED,
            "Scheda grafica",
            "Processore",
            "RAM",
            "SSD",
            "Monitor",
            "Notebook",
            "Sistema operativo",
            "Suite produttività",
            "IDE",
            "Antivirus",
            "Periferica",
            "Accessorio",
            OTHER
    };

    private ProductTaxonomy() {
    }

    static String[] brandFilterOptions(List<String> existingBrands) {
        return filterOptions(ALL_BRANDS, BRAND_OPTIONS, existingBrands);
    }

    static String[] productTypeFilterOptions(List<String> existingTypes) {
        return filterOptions(ALL_TYPES, PRODUCT_TYPE_OPTIONS, existingTypes);
    }

    static boolean containsBrand(String brand) {
        return contains(BRAND_OPTIONS, brand);
    }

    static boolean containsProductType(String productType) {
        return contains(PRODUCT_TYPE_OPTIONS, productType);
    }

    private static String[] filterOptions(String allLabel, String[] baseOptions, List<String> existingValues) {
        Set<String> options = new LinkedHashSet<>();
        options.add(allLabel);
        Arrays.stream(baseOptions)
                .filter(option -> !OTHER.equals(option))
                .forEach(options::add);
        existingValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(options::add);
        return options.toArray(String[]::new);
    }

    private static boolean contains(String[] options, String value) {
        if (value == null) {
            return false;
        }
        return Arrays.stream(options).anyMatch(option -> option.equals(value));
    }
}
