package com.clausify.clause;

import com.clausify.analysis.RiskCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps the standard_clauses table in step with standard-clauses.json at startup: inserts new clauses, updates edited
 * ones, removes deleted ones, and embeds any clause whose text or embedding model changed. Unchanged clauses keep
 * their stored embedding, so a normal restart embeds nothing.
 */
@Component
public class StandardClauseLoader {

    private static final Logger log = LoggerFactory.getLogger(StandardClauseLoader.class);
    static final String LIBRARY = "standard-clauses.json";

    /** One entry in standard-clauses.json. */
    record Spec(String key, RiskCategory category, String title, String text, boolean favorable, String notes) {
    }

    private final StandardClauseRepository repository;
    private final ClauseEmbedder embedder;
    private final ClauseLibrary library;
    private final TransactionTemplate transactions;

    public StandardClauseLoader(StandardClauseRepository repository, ClauseEmbedder embedder, ClauseLibrary library,
                                TransactionTemplate transactions) {
        this.repository = repository;
        this.embedder = embedder;
        this.library = library;
        this.transactions = transactions;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        sync();
    }

    /** Returns how many clauses were embedded. */
    int sync() {
        List<Spec> specs = read();
        int embedded = transactions.execute(status -> {
            Map<String, StandardClause> existing = repository.findAll().stream()
                    .collect(Collectors.toMap(StandardClause::getKey, Function.identity()));
            Set<String> keys = new HashSet<>();
            int count = 0;
            for (Spec spec : specs) {
                keys.add(spec.key());
                StandardClause clause = existing.getOrDefault(spec.key(), new StandardClause(spec.key()));
                boolean textChanged = !Objects.equals(clause.getText(), spec.text());
                clause.setCategory(spec.category());
                clause.setTitle(spec.title());
                clause.setText(spec.text());
                clause.setFavorable(spec.favorable());
                clause.setNotes(spec.notes());
                if (textChanged || clause.getEmbedding() == null || !embedder.modelName().equals(clause.getEmbeddingModel())) {
                    clause.setEmbedding(ClauseLibrary.JSON.writeValueAsString(embedder.embed(spec.text())));
                    clause.setEmbeddingModel(embedder.modelName());
                    count++;
                }
                repository.save(clause);
            }
            existing.values().stream().filter(c -> !keys.contains(c.getKey())).forEach(repository::delete);
            return count;
        });
        library.invalidate();
        log.info("Standard clause library: {} clauses, {} embedded with {}", specs.size(), embedded, embedder.modelName());
        return embedded;
    }

    private static List<Spec> read() {
        try (InputStream in = new ClassPathResource(LIBRARY).getInputStream()) {
            return List.of(ClauseLibrary.JSON.readValue(in, Spec[].class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + LIBRARY, e);
        }
    }
}
