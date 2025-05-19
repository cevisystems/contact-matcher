package com.contactos.demo;

import com.contactos.demo.DemoApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.similarity.LevenshteinDistance;

class DemoApplicationTest {

    @Nested
    class FindPotentialDuplicatesTests {
        @Test
        void testFindPotentialDuplicates_BasicCase() {
            // Preparar datos de prueba
            List<DemoApplication.Contact> contacts = List.of(
                    new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                    new DemoApplication.Contact(2, "John Doe", "JD", "john.doe@example.com", "12345", "123 Main Street"),
                    new DemoApplication.Contact(3, "Jane Smith", "JS", "jane@example.com", "54321", "456 Oak Ave"),
                    new DemoApplication.Contact(4, "Robert Johnson", "RJ", "robert@example.com", "67890", "789 Pine Rd"));

            // Ejecutar función
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);

            // Verificaciones
            assertTrue(results.containsKey(1), "Contacto 1 debería tener duplicados");
            assertEquals(1, results.get(1).size(), "Contacto 1 debería tener 1 duplicado");
            assertEquals(2, results.get(1).get(0).contactId, "Debería coincidir con contacto 2");
            assertTrue(results.get(1).get(0).similarityScore > 0.5, "Score de similitud debería ser mayor que el umbral");

            assertFalse(results.containsKey(3), "Contacto 3 no debería tener duplicados");
            assertFalse(results.containsKey(4), "Contacto 4 no debería tener duplicados");
        }

        @Test
        void testFindPotentialDuplicates_NoDuplicates() {
            List<DemoApplication.Contact> contacts = List.of(
                new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                new DemoApplication.Contact(2, "Jane Smith", "JS", "jane@example.com", "54321", "456 Oak Ave")
            );
            
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);
            assertTrue(results.isEmpty(), "No debería encontrar duplicados");
        }

        @Test
        void testFindPotentialDuplicates_EmailMatchOnly() {
            List<DemoApplication.Contact> contacts = List.of(
                new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                new DemoApplication.Contact(2, "Different Name", "DN", "john@example.com", "54321", "456 Oak Ave")
            );
            
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);
            assertTrue(results.containsKey(1), "Debería encontrar duplicados por email");
            assertEquals("Email similar (100%)", results.get(1).get(0).matchReason);
        }

        @Test
        void testFindPotentialDuplicates_Threshold() {
            List<DemoApplication.Contact> contacts = List.of(
                new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                new DemoApplication.Contact(2, "Jahn Doe", "JD", "different@example.com", "12345", "123 Main St")
            );
            
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);
            assertTrue(results.containsKey(1), "Debería encontrar duplicados aunque no coincida el email");
            assertTrue(results.get(1).get(0).similarityScore >= 0.5, "Score debería superar el umbral");
        }

        @Test
        void testFindPotentialDuplicates_Ordering() {
            List<DemoApplication.Contact> contacts = List.of(
                new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                new DemoApplication.Contact(2, "John Doe", "JD", "john@example.com", "12345", "123 Main St"),
                new DemoApplication.Contact(3, "John D.", "JD", "john@example.com", "12345", "123 Main Street")
            );
            
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);
            List<DemoApplication.DuplicateResult> duplicates = results.get(1);
            
            assertTrue(duplicates.get(0).similarityScore >= duplicates.get(1).similarityScore, 
                     "Los resultados deberían estar ordenados por score descendente");
        }

        @Test
        void testFindPotentialDuplicates_EmptyList() {
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(List.of());
            assertTrue(results.isEmpty(), "Lista vacía no debería producir resultados");
        }

        @Test
        void testFindPotentialDuplicates_SingleContact() {
            List<DemoApplication.Contact> contacts = List.of(
                new DemoApplication.Contact(1, "John Doe", "JD", "john@example.com", "12345", "123 Main St")
            );
            
            Map<Integer, List<DemoApplication.DuplicateResult>> results = DemoApplication.findPotentialDuplicates(contacts);
            assertTrue(results.isEmpty(), "Lista con un solo contacto no debería producir resultados");
        }
    }

    @Nested
    class UtilityMethodsTests {
        @Test
        void testCalculateSimilarity() {
            LevenshteinDistance ld = new LevenshteinDistance();

            // Test casos extremos
            assertEquals(1.0, DemoApplication.calculateSimilarity("", "", ld),
                    "Strings vacíos deberían ser 100% similares");
            assertEquals(1.0, DemoApplication.calculateSimilarity("abc", "abc", ld),
                    "Strings iguales deberían ser 100% similares");
            assertEquals(0.0, DemoApplication.calculateSimilarity("abc", "xyz", ld),
                    "Strings diferentes deberían ser 0% similares");

            // Test caso realista
            double similarity = DemoApplication.calculateSimilarity("hello", "hallo", ld);
            assertTrue(similarity > 0.6 && similarity < 0.9, "Similaridad parcial debería estar en rango esperado");
        }

        @Test
        void testNormalizeString() {
            assertEquals("123mainst", DemoApplication.normalizeString("123 Main St."));
            assertEquals("", DemoApplication.normalizeString(null));
            assertEquals("abc123", DemoApplication.normalizeString("ABC-123!"));
        }
    }
}