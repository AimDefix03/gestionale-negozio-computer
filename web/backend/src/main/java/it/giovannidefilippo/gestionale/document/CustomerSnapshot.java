package it.giovannidefilippo.gestionale.document;

record CustomerSnapshot(
        String code,
        String name,
        String taxCode,
        String vatNumber,
        String email,
        String phone,
        String address,
        String city
) {
    static CustomerSnapshot minimal(String name, String code) {
        return new CustomerSnapshot(optional(code), clean(name), "", "", "", "", "", "");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
