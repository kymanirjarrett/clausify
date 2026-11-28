package com.clausify.clause;

import com.clausify.analysis.RiskCategory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The embedded library, cached in memory by category (ADR 0003: a few hundred vectors, scanned in milliseconds).
 * Built on first use and rebuilt after StandardClauseLoader changes the table.
 */
@Component
public class ClauseLibrary {

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** A library clause with its parsed vector. */
    record Entry(StandardClauseResponse clause, float[] vector) {
    }

    private final StandardClauseRepository repository;
    private volatile Map<RiskCategory, List<Entry>> byCategory;

    public ClauseLibrary(StandardClauseRepository repository) {
        this.repository = repository;
    }

    /** Embedded clauses in the category; clauses not embedded yet are left out. */
    List<Entry> embedded(RiskCategory category) {
        Map<RiskCategory, List<Entry>> cache = byCategory;
        if (cache == null) {
            cache = load();
            byCategory = cache;
        }
        return cache.getOrDefault(category, List.of());
    }

    void invalidate() {
        byCategory = null;
    }

    private Map<RiskCategory, List<Entry>> load() {
        return repository.findAll().stream()
                .filter(c -> c.getEmbedding() != null)
                .map(c -> new Entry(StandardClauseResponse.from(c), JSON.readValue(c.getEmbedding(), float[].class)))
                .collect(Collectors.groupingBy(e -> e.clause().category(), () -> new EnumMap<>(RiskCategory.class),
                        Collectors.toList()));
    }
}
