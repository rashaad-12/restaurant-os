package com.restaurantos.analyticservice.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.restaurantos.analyticservice.dto.request.Condition;
import com.restaurantos.analyticservice.dto.request.ConditionType;
import com.restaurantos.analyticservice.dto.request.FieldOperator;
import com.restaurantos.analyticservice.enums.FieldType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class QueryBuilder {

    public Query build(FieldTypes types, Condition root) {
        if (isNull(root)) return Query.of(q -> q.matchAll(m -> m));

        if (root.isGroup()) return buildGroup(types, root);

        Condition wrapper = new Condition();
        wrapper.setConditionType(ConditionType.AND);
        wrapper.setConditions(List.of(root));
        return buildGroup(types, wrapper);
    }

    private Query buildGroup(FieldTypes types, Condition group) {
        ConditionType type = isNull(group.getConditionType()) ? ConditionType.AND : group.getConditionType();
        boolean or = type == ConditionType.OR;

        Map<String, List<Condition>> nestedByPath = new LinkedHashMap<>();
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean anyShould = false;

        for (Condition child : group.getConditions()) {
            if (isNull(child)) continue;

            if (child.isGroup()) {
                Query sub = buildGroup(types, child);
                if (or) {
                    bool.should(sub);
                    anyShould = true;
                } else {
                    bool.must(sub);
                }
                continue;
            }

            String path = types.nestedPath(child.getField());
            if (nonNull(path)) {
                nestedByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(child);
                continue;
            }

            Query positive = leafPositive(types, child);
            boolean negate = child.getOperator().isNegate();
            if (or) {
                if (negate) bool.should(Query.of(q -> q.bool(b -> b.mustNot(positive))));
                else bool.should(positive);
                anyShould = true;
            } else if (negate) {
                bool.mustNot(positive);
            } else if (child.getOperator().getEsOperator() == FieldOperator.ESOperator.CONTAINS) {
                bool.must(positive);
            } else {
                bool.filter(positive);
            }
        }

        for (Map.Entry<String, List<Condition>> entry : nestedByPath.entrySet()) {
            Query nested = nestedGroup(types, entry.getKey(), entry.getValue(), type);
            if (or) {
                bool.should(nested);
                anyShould = true;
            } else {
                bool.filter(nested);
            }
        }

        if (or && anyShould) {
            bool.minimumShouldMatch("1");
        }
        return bool.build()._toQuery();
    }

    private Query nestedGroup(FieldTypes types, String path, List<Condition> leaves, ConditionType type) {
        BoolQuery.Builder inner = new BoolQuery.Builder();
        boolean anyShould = false;
        for (Condition leaf : leaves) {
            Query positive = leafPositive(types, leaf);
            if (leaf.getOperator().isNegate()) {
                inner.mustNot(positive);
            } else if (type == ConditionType.OR) {
                inner.should(positive);
                anyShould = true;
            } else {
                inner.must(positive);
            }
        }
        if (type == ConditionType.OR && anyShould) {
            inner.minimumShouldMatch("1");
        }
        Query innerQuery = inner.build()._toQuery();
        return Query.of(q -> q.nested(n -> n
                .path(path)
                .query(innerQuery)
                .scoreMode(ChildScoreMode.None)));
    }

    private Query leafPositive(FieldTypes types, Condition leaf) {
        String field = leaf.getField();
        FieldType type = types.typeOf(field);
        FieldOperator op = leaf.getOperator();
        List<String> values = expand(leaf.getValues());

        return switch (op.getEsOperator()) {
            case EXISTS -> exists(field);
            case RANGE_BETWEEN -> range(field, type, value(values, 0), value(values, 1));
            case RANGE_GTE -> range(field, type, value(values, 0), null);
            case RANGE_LTE -> range(field, type, null, value(values, 0));
            case RANGE_GT -> rangeStrict(field, type, value(values, 0), null);
            case RANGE_LT -> rangeStrict(field, type, null, value(values, 0));
            case TERM -> termQuery(field, type, values);
            default -> matchQuery(field, type, op, values, leaf.getValueCondition());
        };
    }

    private Query termQuery(String field, FieldType type, List<String> values) {
        if (values.isEmpty()) return matchNone();

        if (type == FieldType.TEXT) {
            if (values.size() == 1) return matchAnd(field, values.get(0));

            BoolQuery.Builder bool = new BoolQuery.Builder();
            for (String v : values) {
                bool.should(matchAnd(field, v));
            }

            bool.minimumShouldMatch("1");
            return bool.build()._toQuery();
        }
        List<FieldValue> fieldValues = values.stream().map(v -> fieldValue(type, v)).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
    }

    private Query matchQuery(String field, FieldType type, FieldOperator op,
                             List<String> values, ConditionType valueCondition) {
        if (values.isEmpty()) return matchNone();

        if (values.size() == 1) return leafValue(field, type, op, values.get(0));

        ConditionType combine = valueCondition == null ? ConditionType.OR : valueCondition;
        BoolQuery.Builder bool = new BoolQuery.Builder();
        for (String v : values) {
            Query q = leafValue(field, type, op, v);
            if (combine == ConditionType.AND) {
                bool.must(q);
            } else {
                bool.should(q);
            }

        }
        if (combine == ConditionType.OR) bool.minimumShouldMatch("1");
        return bool.build()._toQuery();
    }

    private Query leafValue(String field, FieldType type, FieldOperator op, String value) {
        boolean text = type == FieldType.TEXT;
        return switch (op.getEsOperator()) {
            case CONTAINS -> text ? match(field, value) : wildcard(field, "*" + escape(value) + "*");
            case PREFIX -> text ? matchPhrasePrefix(field, value) : prefix(field, value);
            case SUFFIX -> wildcard(field, "*" + escape(value));
            default -> throw new IllegalStateException("Unsupported leaf kind: " + op.getEsOperator());
        };
    }

    private Query matchNone() {
        return Query.of(q -> q.matchNone(m -> m));
    }

    private Query match(String field, String value) {
        return Query.of(q -> q.match(m -> m.field(field).query(value)));
    }

    private Query matchAnd(String field, String value) {
        return Query.of(q -> q.match(m -> m.field(field).query(value).operator(Operator.And)));
    }

    private Query matchPhrasePrefix(String field, String value) {
        return Query.of(q -> q.matchPhrasePrefix(m -> m.field(field).query(value)));
    }

    private Query prefix(String field, String value) {
        return Query.of(q -> q.prefix(p -> p.field(field).value(value)));
    }

    private Query wildcard(String field, String pattern) {
        return Query.of(q -> q.wildcard(w -> w.field(field).value(pattern).caseInsensitive(true)));
    }

    private Query exists(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    private Query range(String field, FieldType type, String gte, String lte) {
        return Query.of(q -> q.range(r -> {
            if (type == FieldType.DATE) {
                return r.date(d -> {
                    d.field(field);
                    if (gte != null) d.gte(gte);
                    if (lte != null) d.lte(lte);
                    return d;
                });
            }
            return r.number(n -> {
                n.field(field);
                if (gte != null) n.gte(Double.parseDouble(gte));
                if (lte != null) n.lte(Double.parseDouble(lte));
                return n;
            });
        }));
    }

    private Query rangeStrict(String field, FieldType type, String gt, String lt) {
        return Query.of(q -> q.range(r -> {
            if (type == FieldType.DATE) {
                return r.date(d -> {
                    d.field(field);
                    if (gt != null) d.gt(gt);
                    if (lt != null) d.lt(lt);
                    return d;
                });
            }
            return r.number(n -> {
                n.field(field);
                if (gt != null) n.gt(Double.parseDouble(gt));
                if (lt != null) n.lt(Double.parseDouble(lt));
                return n;
            });
        }));
    }

    private FieldValue fieldValue(FieldType type, String value) {
        return switch (type) {
            case LONG -> FieldValue.of(Long.parseLong(value));
            case DOUBLE -> FieldValue.of(Double.parseDouble(value));
            case BOOLEAN -> FieldValue.of(Boolean.parseBoolean(value));
            default -> FieldValue.of(value);
        };
    }

    private List<String> expand(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v == null) continue;
            for (String part : v.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) out.add(trimmed);
            }
        }
        return out;
    }

    private String value(List<String> values, int index) {
        return index < values.size() ? values.get(index) : null;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }
}
