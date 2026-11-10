package com.clausify.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the model's JSON reply into a {@link ContractAnalysisResult}, or rejects it. Model output is untrusted:
 * Groq's strict schema mode guarantees the shape, but not ranges or lengths, so every rule is checked here and any
 * violation rejects the whole reply. Nothing is clamped or truncated, so an invalid reply can never turn into a
 * plausible-looking result. Length limits match the database columns (V6) and keep the UI readable.
 */
final class ModelOutputParser {

    static final int MAX_CONTRACT_TYPE = 100;
    static final int MAX_SUMMARY = 1000;
    static final int MAX_TITLE = 255;
    static final int MAX_SECTION_REF = 100;
    static final int MAX_ORIGINAL_TEXT = 5000;
    static final int MAX_EXPLANATION = 2000;
    static final int MAX_REVISION = 2000;

    private static final Set<String> TOP_LEVEL = Set.of("contractType", "summary", "findings");
    private static final Set<String> FINDING_FIELDS = Set.of("category", "riskScore", "favorable", "title",
            "sectionRef", "originalText", "explanation", "suggestedRevision");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ModelOutputParser() {
    }

    static ContractAnalysisResult parse(String content, int maxFindings) throws InvalidModelOutputException {
        if (content == null || content.isBlank()) {
            throw new InvalidModelOutputException("empty reply");
        }
        JsonNode root;
        try {
            root = JSON.readTree(content);
        } catch (JacksonException e) {
            throw new InvalidModelOutputException("reply is not valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidModelOutputException("reply is not a JSON object");
        }
        requireOnly(root, TOP_LEVEL, "reply");

        String contractType = optionalString(root, "contractType", MAX_CONTRACT_TYPE);
        String summary = optionalString(root, "summary", MAX_SUMMARY);

        JsonNode findingsNode = root.get("findings");
        if (findingsNode == null || !findingsNode.isArray()) {
            throw new InvalidModelOutputException("findings is missing or not an array");
        }
        if (findingsNode.size() > maxFindings) {
            throw new InvalidModelOutputException("too many findings: " + findingsNode.size());
        }
        List<ContractAnalysisResult.Finding> findings = new ArrayList<>();
        for (JsonNode node : findingsNode) {
            findings.add(finding(node));
        }
        return new ContractAnalysisResult(contractType, summary, findings);
    }

    private static ContractAnalysisResult.Finding finding(JsonNode node) throws InvalidModelOutputException {
        if (!node.isObject()) {
            throw new InvalidModelOutputException("finding is not an object");
        }
        requireOnly(node, FINDING_FIELDS, "finding");

        JsonNode score = node.get("riskScore");
        if (score == null || !score.isIntegralNumber() || !score.canConvertToInt()
                || score.intValue() < 0 || score.intValue() > 100) {
            throw new InvalidModelOutputException("riskScore is not a whole number from 0 to 100");
        }
        JsonNode favorable = node.get("favorable");
        if (favorable == null || !favorable.isBoolean()) {
            throw new InvalidModelOutputException("favorable is not a boolean");
        }
        return new ContractAnalysisResult.Finding(
                category(node.get("category")),
                score.intValue(),
                favorable.booleanValue(),
                requiredString(node, "title", MAX_TITLE),
                optionalString(node, "sectionRef", MAX_SECTION_REF),
                requiredString(node, "originalText", MAX_ORIGINAL_TEXT),
                requiredString(node, "explanation", MAX_EXPLANATION),
                optionalString(node, "suggestedRevision", MAX_REVISION));
    }

    private static RiskCategory category(JsonNode node) throws InvalidModelOutputException {
        if (node == null || !node.isString()) {
            throw new InvalidModelOutputException("category is missing");
        }
        try {
            return RiskCategory.valueOf(node.stringValue()); // exact names only, no case folding
        } catch (IllegalArgumentException e) {
            throw new InvalidModelOutputException("unknown category");
        }
    }

    private static void requireOnly(JsonNode node, Set<String> allowed, String what) throws InvalidModelOutputException {
        for (String name : node.propertyNames()) {
            if (!allowed.contains(name)) {
                throw new InvalidModelOutputException(what + " has an unexpected field");
            }
        }
    }

    private static String requiredString(JsonNode parent, String field, int maxLength)
            throws InvalidModelOutputException {
        String value = optionalString(parent, field, maxLength);
        if (value == null || value.isBlank()) {
            throw new InvalidModelOutputException(field + " is missing or blank");
        }
        return value;
    }

    private static String optionalString(JsonNode parent, String field, int maxLength)
            throws InvalidModelOutputException {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isString()) {
            throw new InvalidModelOutputException(field + " is not a string");
        }
        String value = node.stringValue().strip();
        if (value.length() > maxLength) {
            throw new InvalidModelOutputException(field + " is longer than " + maxLength + " characters");
        }
        return value.isEmpty() ? null : value;
    }
}
