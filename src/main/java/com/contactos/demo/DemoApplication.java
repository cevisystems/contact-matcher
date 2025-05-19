package com.contactos.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.*;

@SpringBootApplication
public class DemoApplication {

    // Punto de entrada de la aplicación Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);

        // 1. Cargar contactos desde archivo Excel
        String excelFileName = "contactos.xlsx";
        List<Contact> contacts = readContactsFromExcel(excelFileName);

        if (contacts.isEmpty()) {
            System.out.println("No se encontraron contactos en el archivo.");
            return;
        }

        // 2. Mostrar contactos cargados
        System.out.println("\n=== CONTACTOS CARGADOS ===");
        contacts.forEach(contact -> System.out.printf("%3d: %s%n", contact.contactId, contact.name));
        System.out.println();

        // 3. Buscar y mostrar duplicados potenciales
        Map<Integer, List<DuplicateResult>> duplicates = findPotentialDuplicates(contacts);
        printDuplicateResults(duplicates);
    }

    /**
     * Lee contactos desde un archivo Excel en el classpath
     * 
     * @param fileName Nombre del archivo Excel en resources/
     * @return Lista de contactos cargados
     */
    public static List<Contact> readContactsFromExcel(String fileName) {
        List<Contact> contacts = new ArrayList<>();

        try {
            // Cargar recurso desde classpath
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) {
                System.err.println("Archivo no encontrado: " + fileName);
                return contacts;
            }

            // Procesar archivo Excel usando Apache POI
            try (InputStream is = resource.getInputStream();
                    Workbook workbook = new XSSFWorkbook(is)) {

                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                // Saltar fila de encabezados
                if (rowIterator.hasNext())
                    rowIterator.next();

                // Leer cada fila y crear objetos Contact
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Contact contact = new Contact(
                            (int) row.getCell(0).getNumericCellValue(), // ID
                            getStringValue(row.getCell(1)), // Nombre
                            getStringValue(row.getCell(2)), // Nombre alternativo
                            getStringValue(row.getCell(3)), // Email
                            getStringValue(row.getCell(4)), // Código postal
                            getStringValue(row.getCell(5))); // Dirección
                    contacts.add(contact);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al leer el archivo Excel: " + e.getMessage());
        }

        return contacts;
    }

    /**
     * Helper para obtener valores de celdas Excel consistentemente
     */
    private static String getStringValue(Cell cell) {
        if (cell == null)
            return null;

        // Convertir diferentes tipos de celdas a String
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    /**
     * Clase interna que representa un contacto
     */
    static class Contact {
        int contactId; // Identificador único
        String name; // Nombre completo
        String name1; // Nombre alternativo (iniciales, etc.)
        String email; // Dirección de email
        String postalZip; // Código postal
        String address; // Dirección física

        public Contact(int contactId, String name, String name1, String email, String postalZip, String address) {
            this.contactId = contactId;
            this.name = name;
            this.name1 = name1;
            this.email = email;
            this.postalZip = postalZip;
            this.address = address;
        }
    }

    /**
     * Clase interna que representa un resultado de duplicado
     */
    static class DuplicateResult {
        int contactId; // ID del contacto duplicado
        double similarityScore; // Puntuación de similitud (0-1)
        String matchReason; // Razón de la coincidencia

        public DuplicateResult(int contactId, double similarityScore, String matchReason) {
            this.contactId = contactId;
            this.similarityScore = similarityScore;
            this.matchReason = matchReason;
        }
    }

    /**
     * Busca contactos potencialmente duplicados usando algoritmo de similitud
     * 
     * @param contacts Lista de contactos a analizar
     * @return Mapa con los resultados (clave: ID contacto, valor: lista de
     *         duplicados)
     */
    public static Map<Integer, List<DuplicateResult>> findPotentialDuplicates(List<Contact> contacts) {
        Map<Integer, List<DuplicateResult>> duplicatesMap = new HashMap<>();
        LevenshteinDistance levenshtein = new LevenshteinDistance();

        // Comparar cada contacto con todos los demás
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact1 = contacts.get(i);
            List<DuplicateResult> duplicates = new ArrayList<>();

            for (int j = 0; j < contacts.size(); j++) {
                if (i == j)
                    continue; // No comparar consigo mismo

                Contact contact2 = contacts.get(j);
                List<String> matchReasons = new ArrayList<>();
                double score = 0;

                // 1. Comparación de nombres (40% del score total)
                if (contact1.name != null && contact2.name != null) {
                    double nameSimilarity = calculateSimilarity(contact1.name, contact2.name, levenshtein);
                    if (nameSimilarity > 0.7) {
                        score += nameSimilarity * 0.4;
                        matchReasons.add("Nombre similar (" + String.format("%.0f", nameSimilarity * 100) + "%)");
                    }
                }

                // 2. Comparación de emails (30% del score total)
                if (contact1.email != null && contact2.email != null &&
                        !"null".equalsIgnoreCase(contact1.email) && !"null".equalsIgnoreCase(contact2.email)) {
                    double emailSimilarity = calculateSimilarity(contact1.email, contact2.email, levenshtein);
                    if (emailSimilarity > 0.8) {
                        score += emailSimilarity * 0.3;
                        matchReasons.add("Email similar (" + String.format("%.0f", emailSimilarity * 100) + "%)");
                    }
                }

                // 3. Comparación de direcciones (20% del score total)
                if (contact1.address != null && contact2.address != null) {
                    double addressSimilarity = calculateSimilarity(
                            normalizeString(contact1.address),
                            normalizeString(contact2.address),
                            levenshtein);
                    if (addressSimilarity > 0.6) {
                        score += addressSimilarity * 0.2;
                        matchReasons.add("Dirección similar (" + String.format("%.0f", addressSimilarity * 100) + "%)");
                    }
                }

                // 4. Comparación de códigos postales (10% del score total)
                if (contact1.postalZip != null && contact2.postalZip != null &&
                        contact1.postalZip.equals(contact2.postalZip)) {
                    score += 0.1;
                    matchReasons.add("Mismo código postal");
                }

                // Si el score supera el umbral, agregar a resultados
                if (score >= 0.5) {
                    duplicates.add(new DuplicateResult(
                            contact2.contactId,
                            score,
                            String.join(", ", matchReasons)));
                }
            }

            if (!duplicates.isEmpty()) {
                // Ordenar duplicados por score descendente
                duplicates.sort((a, b) -> Double.compare(b.similarityScore, a.similarityScore));
                duplicatesMap.put(contact1.contactId, duplicates);
            }
        }

        return duplicatesMap;
    }

    /**
     * Normaliza strings para comparación (elimina espacios, caracteres especiales,
     * etc.)
     */
    public static String normalizeString(String input) {
        if (input == null)
            return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]", "") // Eliminar caracteres no alfanuméricos
                .replaceAll("\\s+", ""); // Eliminar espacios
    }

    /**
     * Calcula similitud entre dos strings usando distancia de Levenshtein
     * 
     * @return Valor entre 0 (diferentes) y 1 (idénticos)
     */
    public static double calculateSimilarity(String s1, String s2, LevenshteinDistance levenshtein) {
        if (s1 == null || s2 == null)
            return 0;
        if (s1.equalsIgnoreCase(s2))
            return 1.0;

        // Normalizar strings antes de comparar
        String norm1 = normalizeString(s1);
        String norm2 = normalizeString(s2);
        if (norm1.isEmpty() || norm2.isEmpty())
            return 0;

        // Calcular distancia de edición y convertir a porcentaje de similitud
        int maxLen = Math.max(norm1.length(), norm2.length());
        int distance = levenshtein.apply(norm1, norm2);

        return 1.0 - (double) distance / maxLen;
    }

    /**
     * Imprime resultados de detección de duplicados en formato tabular
     */
    private static void printDuplicateResults(Map<Integer, List<DuplicateResult>> duplicates) {
        System.out.println("======================================================");
        System.out.println("          DETECCIÓN DE CONTACTOS DUPLICADOS          ");
        System.out.println("======================================================");
        System.out.printf("%-20s %-20s %-15s%n", "ContactID Origen", "ContactID Coincidencia", "Precisión");
        System.out.println("------------------------------------------------------");

        if (duplicates.isEmpty()) {
            System.out.println("No se encontraron posibles duplicados.");
            return;
        }

        // Ordenar e imprimir resultados
        duplicates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int contactIdOrigen = entry.getKey();
                    List<DuplicateResult> matches = entry.getValue();

                    matches.forEach(match -> {
                        String precision = match.similarityScore >= 0.75 ? "Alta" : "Baja";
                        System.out.printf("%-20d %-20d %-15s%n",
                                contactIdOrigen,
                                match.contactId,
                                precision);
                    });
                });

        System.out.println("======================================================");
        System.out.printf("Total de coincidencias encontradas: %d%n",
                duplicates.values().stream().mapToInt(List::size).sum());
    }
}