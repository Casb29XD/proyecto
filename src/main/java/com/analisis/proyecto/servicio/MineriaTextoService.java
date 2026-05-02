package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la extracción de frecuencias y descubrimiento de términos
 * (Text Mining) utilizando NLP básico y métricas de precisión contextual.
 * 
 * Fórmula Matemática de Precisión Contextual:
 * 
 *                 Co-ocurrencias(w, ContextoBase)
 * Precisión(w) = ─────────────────────────────────
 *                       Frecuencia_Total(w)
 * 
 * Donde:
 * - Co-ocurrencias(w, ContextoBase): Número de veces que la palabra "w" aparece 
 *   en un documento que también contiene al menos una palabra de la categoría base.
 * - Frecuencia_Total(w): Total de veces que "w" aparece en todos los documentos.
 * 
 * Diagrama de Extracción y Co-ocurrencia:
 * 
 * Abstract: "The generative models are trained using novel backpropagation..."
 * 
 * 1. Filtro Stopwords: [generative, models, trained, novel, backpropagation]
 * 2. Match Base: "generative models" (Encontrado -> Incrementa Co-ocurrencias)
 * 3. Nuevos Términos: "trained", "novel", "backpropagation"
 * 
 * Si "backpropagation" aparece 10 veces en total, pero 8 de esas veces
 * aparece junto a términos base, su precisión es 0.8 (80% relevante al dominio).
 *
 */
@Service
public class MineriaTextoService {

    // Diccionario predefinido (Requerimiento 3) -> Todo a minúsculas
    private static final List<String> CATEGORIA_BASE = Arrays.asList(
        "generative models", "prompting", "machine learning", "multimodality", 
        "fine-tuning", "training data", "algorithmic bias", "explainability", 
        "transparency", "ethics", "privacy", "personalization", 
        "human-ai interaction", "ai literacy", "co-creation"
    );

    // Stopwords extendidos: inglés académico + palabras del propio query de búsqueda
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        // Artículos, preposiciones, conjunciones 
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "by",
        "of", "from", "as", "is", "are", "was", "were", "be", "been", "this", "that", "these",
        "those", "it", "they", "we", "can", "could", "will", "would", "which", "how", "may",
        "what", "where", "our", "their", "its", "has", "have", "had", "not", "no", "such",
        "also", "using", "used", "based", "through", "between", "both", "all", "any", "each",
        "into", "about", "more", "most", "than", "do", "does", "did", "some", "other", "over",
        "when", "while", "then", "thus", "however", "therefore", "moreover", "furthermore",
        "although", "because", "since", "until", "after", "before", "during", "within",
        "should", "must", "need", "only", "still", "even", "well", "just", "much", "very",
        "being", "make", "made", "like", "new", "first", "one", "two", "three", "many",
        "often", "already", "show", "shown", "shows", "provide", "provides", "include",
        "including", "includes", "present", "presents", "propose", "proposed", "several",
        "different", "important", "significant", "specific", "particular", "given", "whether",
        "across", "among", "along", "against", "rather", "further", "yet", "per", "upon",
        // Verbos y formas genéricas académicas
        "use", "work", "works", "find", "found", "report", "reported", "suggest", "suggests",
        "examine", "explore", "discuss", "consider", "address", "investigate", "develop",
        "identify", "demonstrate", "indicate", "highlight", "enable", "allows", "allow",
        "offer", "offers", "focus", "aims", "aim", "lead", "leads", "help", "ensure",
        // Sustantivos genéricos de papers científicos
        "model", "models", "data", "system", "systems", "approach", "paper", "study",
        "method", "methods", "result", "results", "process", "information", "analysis",
        "research", "application", "applications", "framework", "technology", "technologies",
        "tools", "tool", "performance", "design", "development", "user", "users", "task",
        "tasks", "field", "area", "context", "role", "time", "case", "cases", "form",
        "level", "type", "types", "part", "number", "range", "example", "order", "terms",
        "content", "knowledge", "understanding", "review", "article", "articles", "literature",
        "potential", "impact", "challenges", "current", "state", "future", "existing",
        "recent", "previous", "related", "various", "key", "wide", "high", "large", "small",
        "novel", "effective", "complex", "emerging", "innovative", "innovations", "academic",
        "ability", "quality", "value", "world", "real", "human", "social", "practice",
        // Palabras del propio query "generative artificial intelligence" — ya son el tema, no descubrimientos
        "generative", "artificial", "intelligence", "generated"
    ));

    public record PalabraFrecuencia(String palabra, int frecuencia) {}
    
    public record PalabraDescubierta(String palabra, int frecuencia, double precision) {}

    public record ResultadoMineria(
        List<PalabraFrecuencia> palabrasBase, 
        List<PalabraDescubierta> nuevasPalabras
    ) {}

    public ResultadoMineria analizarFrecuencias(List<Articulo> articulos) {
        if (articulos == null || articulos.isEmpty()) {
            return new ResultadoMineria(new ArrayList<>(), new ArrayList<>());
        }

        Map<String, Integer> freqBase = new HashMap<>();
        Map<String, Integer> freqNuevos = new HashMap<>();
        
        // Co-ocurrencias de vocablos nuevos con las palabras Base preestablecidas
        // Cuenta en cuántos abstracts apareció la palabra nueva AL LADO (mismo texto) que un target base.
        Map<String, Integer> coOcurrencias = new HashMap<>();

        // Inicializar frecuencias de categoría base en 0
        for (String c : CATEGORIA_BASE) {
            freqBase.put(c, 0);
        }

        for (Articulo articulo : articulos) {
            String abstractRaw = articulo.getResumen();
            if (abstractRaw == null || abstractRaw.trim().isEmpty()) {
                continue;
            }
            
            // Convertimos a minúsculas para homologar
            String text = abstractRaw.toLowerCase();
            
            // 1. Escanear palabras clave base exactas y subcadenas compuestas
            boolean contieneAlgunaBase = false;
            for (String baseWord : CATEGORIA_BASE) {
                // Buscamos repeticiones independientes usando regex ignorando símbolos pegados.
                int matchesCount = text.split("\\b" + java.util.regex.Pattern.quote(baseWord) + "\\b").length - 1;
                if (matchesCount > 0) {
                    freqBase.put(baseWord, freqBase.get(baseWord) + matchesCount);
                    contieneAlgunaBase = true;
                }
            }

            // 2. Extracción de token individuales para nuevas palabras  (Unigramas simples)
            // Quitamos puntuación 
            String cleanedText = text.replaceAll("[^a-z\\s-]", " ");
            String[] tokens = cleanedText.split("\\s+");
            
            Set<String> tokensUnicosDelAbstract = new HashSet<>();

            for (String token : tokens) {
                token = token.trim();
                // Ignorar stopwords, palabras muy pequeñas, o aquellas que ya están en el diccionario base
                if (token.length() > 4 
                    && !STOP_WORDS.contains(token) 
                    && !esCombinacionEnBase(token)) {
                    
                    freqNuevos.put(token, freqNuevos.getOrDefault(token, 0) + 1);
                    tokensUnicosDelAbstract.add(token);
                }
            }

            // Si este abstract contenía una de las palabras fundamentales, aumenta la co-ocurrencia para evaluación matemática
            if (contieneAlgunaBase) {
                for (String t : tokensUnicosDelAbstract) {
                    coOcurrencias.put(t, coOcurrencias.getOrDefault(t, 0) + 1);
                }
            }
        }

        // Mapear Base formatedo
        List<PalabraFrecuencia> topBase = freqBase.entrySet().stream()
            .map(e -> new PalabraFrecuencia(e.getKey(), e.getValue()))
            .sorted((a, b) -> Integer.compare(b.frecuencia(), a.frecuencia())) // Descending
            .collect(Collectors.toList());

        // Filtrar y mapear Nuevas con Precisión
        // Seleccionamos las 15 más frecuentes que logran el mayor factor de co-ocurrencia
        List<PalabraDescubierta> topNuevas = freqNuevos.entrySet().stream()
            .filter(e -> e.getValue() >= 2) // Al menos 2 apariciones para ser relevante
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Sort by generic frequency
            .limit(100) // Procesar el Top 100 de puras frecuencias al calcular la métrica más pesada
            .map(e -> {
                String word = e.getKey();
                int freqTotal = e.getValue();
                int vecesCoocurre = coOcurrencias.getOrDefault(word, 0);
                
                // Métrica Requerimiento 3: Precisión = Co-ocurrencias / Frecuencia Total de la Palabra (en los documentos analizados)
                // Si la palabra aparece 10 veces en TODO el DB, y 10 veces en párrafos de "Generative AI", precisión = 1.0 (Exacta, fuertemente ligada)
                double precision = freqTotal > 0 ? (double) vecesCoocurre / freqTotal : 0.0;
                
                return new PalabraDescubierta(word, freqTotal, Math.round(precision * 1000.0) / 1000.0);
            })
            .sorted((a, b) -> Double.compare(b.precision() * b.frecuencia(), a.precision() * a.frecuencia())) // Ordenamos por un score ponderado de descubrimientos precisos y frecuentes
            .limit(15) // Requerimiento pide extraer un listado ("mínimo de palabras asociadas (máximo 15)")
            .collect(Collectors.toList());

        return new ResultadoMineria(topBase, topNuevas);
    }

    public ResultadoMineria analizarFrecuenciasDocumento(Articulo articulo) {
        if (articulo == null || articulo.getResumen() == null || articulo.getResumen().trim().isEmpty()) {
            return new ResultadoMineria(new ArrayList<>(), new ArrayList<>());
        }

        Map<String, Integer> freqBase = new HashMap<>();
        Map<String, Integer> freqNuevos = new HashMap<>();
        Map<String, Integer> coOcurrencias = new HashMap<>();

        for (String c : CATEGORIA_BASE) {
            freqBase.put(c, 0);
        }

        String text = articulo.getResumen().toLowerCase();
        boolean contieneAlgunaBase = false;
        
        for (String baseWord : CATEGORIA_BASE) {
            int matchesCount = text.split("\\b" + java.util.regex.Pattern.quote(baseWord) + "\\b").length - 1;
            if (matchesCount > 0) {
                freqBase.put(baseWord, freqBase.get(baseWord) + matchesCount);
                contieneAlgunaBase = true;
            }
        }

        String cleanedText = text.replaceAll("[^a-z\\s-]", " ");
        String[] tokens = cleanedText.split("\\s+");
        Set<String> tokensUnicos = new HashSet<>();

        for (String token : tokens) {
            token = token.trim();
            if (token.length() > 4 && !STOP_WORDS.contains(token) && !esCombinacionEnBase(token)) {
                freqNuevos.put(token, freqNuevos.getOrDefault(token, 0) + 1);
                tokensUnicos.add(token);
            }
        }

        if (contieneAlgunaBase) {
            for (String t : tokensUnicos) {
                coOcurrencias.put(t, coOcurrencias.getOrDefault(t, 0) + 1);
            }
        }

        List<PalabraFrecuencia> topBase = freqBase.entrySet().stream()
            .map(e -> new PalabraFrecuencia(e.getKey(), e.getValue()))
            .sorted((a, b) -> Integer.compare(b.frecuencia(), a.frecuencia()))
            .collect(Collectors.toList());

        List<PalabraDescubierta> topNuevas = freqNuevos.entrySet().stream()
            .filter(e -> e.getValue() >= 1) // En un solo documento, 1 aparición es válida
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(100)
            .map(e -> {
                String word = e.getKey();
                int freqTotal = e.getValue();
                int vecesCoocurre = coOcurrencias.getOrDefault(word, 0);
                double precision = freqTotal > 0 ? (double) vecesCoocurre / freqTotal : 0.0;
                return new PalabraDescubierta(word, freqTotal, Math.round(precision * 1000.0) / 1000.0);
            })
            .sorted((a, b) -> Double.compare(b.precision() * b.frecuencia(), a.precision() * a.frecuencia()))
            .limit(15)
            .collect(Collectors.toList());

        return new ResultadoMineria(topBase, topNuevas);
    }

    private boolean esCombinacionEnBase(String token) {
        for (String c : CATEGORIA_BASE) {
            if (c.contains(token)) return true;
        }
        return false;
    }
}
