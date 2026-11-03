package org.jahia.modules.htmlfiltering.report;

import org.jahia.modules.htmlfiltering.PolicySanitizedHtmlResult;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemCollectorTest {

    @Test
    public void GIVEN_a_limit_WHEN_adding_more_items_THEN_the_collector_keeps_the_first_items_only() {
        ItemCollector collector = new ItemCollector(2);

        collector.add("/sites/a/1", "text", null, List.of("<script>"));
        assertFalse(collector.isFull());
        collector.add("/sites/a/2", "text", "fr", List.of("<script>"));
        assertTrue(collector.isFull());
        collector.add("/sites/a/3", "text", null, List.of("<script>"));

        assertEquals(2, collector.getItems().size());
        assertEquals("/sites/a/2", collector.getItems().get(1).getPath());
        assertEquals("fr", collector.getItems().get(1).getLanguage());
    }

    @Test
    public void GIVEN_a_valid_value_WHEN_adding_it_THEN_the_collector_ignores_it() {
        ItemCollector collector = new ItemCollector(1);

        collector.add("/sites/a/1", "text", null, Collections.emptyList());

        assertTrue(collector.getItems().isEmpty());
        assertFalse(collector.isFull());
    }

    @Test
    public void GIVEN_rejected_tags_and_attributes_WHEN_reading_the_markups_THEN_tags_come_first_and_each_list_is_sorted() {
        PolicySanitizedHtmlResult result = mock(PolicySanitizedHtmlResult.class);
        when(result.getRejectedTags()).thenReturn(Set.of("script", "iframe"));
        when(result.getRejectedAttributesByTag()).thenReturn(Map.of("img", Set.of("onerror", "onclick"), "a", Set.of("onclick")));

        assertEquals(List.of("<iframe>", "<script>", "<a onclick>", "<img onclick>", "<img onerror>"), ItemCollector.markupsOf(result));
    }
}
