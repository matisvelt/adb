package com.antlab.rigcontrol.sorter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RulesEngineTests {
    @Test
    void parsesAndEvaluatesConditions() {
        RulesEngine engine = new RulesEngine();
        Rule rule = new Rule("r1", true, "label == DOCUMENT_INVOICE", "Work/Invoices", "invoice");
        engine.setRules(List.of(rule));

        FileRecord record = new FileRecord();
        record.setLabel("DOCUMENT_INVOICE");
        record.setConfidence(0.9);

        RulesEngine.RuleMatch match = engine.evaluate(record);
        assertNotNull(match);
        assertEquals("Work/Invoices", match.getDestination());
    }

    @Test
    void supportsNumericComparisons() {
        RulesEngine engine = new RulesEngine();
        Rule rule = new Rule("r2", true, "facesCount >= 1 && confidence >= 0.75", "People", "people");
        engine.setRules(List.of(rule));

        FileRecord record = new FileRecord();
        record.setFacesCount(2);
        record.setConfidence(0.8);

        assertNotNull(engine.evaluate(record));
    }

    @Test
    void supportsStartsWith() {
        RulesEngine engine = new RulesEngine();
        Rule rule = new Rule("r3", true, "label startsWith PHOTO", "Photos", "photos");
        engine.setRules(List.of(rule));

        FileRecord record = new FileRecord();
        record.setLabel("PHOTO_PEOPLE");

        assertNotNull(engine.evaluate(record));
    }
}
