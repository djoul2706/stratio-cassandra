/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.query;

import com.google.common.base.Objects;
import com.stratio.cassandra.index.schema.Schema;
import com.stratio.cassandra.util.JsonSerializer;
import com.stratio.cassandra.util.Log;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.lucene.search.*;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Class representing an Lucene index search. It is formed by an optional querying {@link Condition} and an optional
 * filtering {@link Condition}. It can be translated to a Lucene {@link Query} using a {@link Schema}.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class Search {

    /** he {@link Condition} for querying, maybe {@code null} meaning no querying. */
    @JsonProperty("query")
    private Condition queryCondition;

    /** The {@link Condition} for filtering, maybe {@code null} meaning no filtering. */
    @JsonProperty("filter")
    private Condition filterCondition;

    /**
     * The {@link Sort} for the query. Note that is the order in which the data will be read before querying, not the
     * order of the results after querying.
     */
    @JsonProperty("sort")
    private Sort sort;

    /**
     * Returns a new {@link Search} composed by the specified querying and filtering conditions.
     *
     * @param queryCondition  The {@link Condition} for querying, maybe {@code null} meaning no querying.
     * @param filterCondition The {@link Condition} for filtering, maybe {@code null} meaning no filtering.
     * @param sort            The {@link Sort} for the query. Note that is the order in which the data will be read
     *                        before querying, not the order of the results after querying.
     */
    @JsonCreator
    public Search(@JsonProperty("query") Condition queryCondition,
                  @JsonProperty("filter") Condition filterCondition,
                  @JsonProperty("sort") Sort sort) {
        this.queryCondition = queryCondition;
        this.filterCondition = filterCondition;
        this.sort = sort;
    }

    /**
     * Returns a new {@link Search} from the specified JSON {@code String}.
     *
     * @param json A JSON {@code String} representing a {@link Search}.
     * @return The {@link Search} represented by the specified JSON {@code String}.
     */
    public static Search fromJson(String json) {
        try {
            return JsonSerializer.fromString(json, Search.class);
        } catch (Exception e) {
            String message = String.format("Unparseable JSON search: %s", e.getMessage());
            Log.error(e, message);
            throw new IllegalArgumentException(message, e);
        }
    }

    /**
     * Returns the JSON representation of this object.
     *
     * @return the JSON representation of this object.
     */
    public String toJson() {
        try {
            return JsonSerializer.toString(this);
        } catch (Exception e) {
            String message = String.format("Unformateable JSON search: %s", e.getMessage());
            Log.error(e, message);
            throw new IllegalArgumentException(message, e);
        }
    }

    /**
     * Returns {@code true} if the results must be ordered by relevance. If {@code false}, then the results are sorted
     * by the natural Cassandra's order. Results must be ordered by relevance if the querying condition is not {code
     * null}.
     * <p/>
     * Relevance is used when the query condition is set, and it is not used when only the clusteringKeyFilter condition
     * is set.
     *
     * @return {@code true} if the results must be ordered by relevance. If {@code false}, then the results must be
     * sorted by the natural Cassandra's order.
     */
    public boolean usesRelevanceOrSorting() {
        return queryCondition != null || sort != null;
    }

    /**
     * Returns {@code true} if this search uses Lucene relevance formula, {@code false} otherwise.
     *
     * @return {@code true} if this search uses Lucene relevance formula, {@code false} otherwise.
     */
    public boolean usesRelevance() {
        return queryCondition != null;
    }

    /**
     * Returns {@code true} if this search uses field sorting, {@code false} otherwise.
     *
     * @return {@code true} if this search uses field sorting, {@code false} otherwise.
     */
    public boolean usesSorting() {
        return sort != null;
    }

    /**
     * Returns the field sorting to be used, maybe {@code null} meaning no field sorting.
     *
     * @return The field sorting to be used, maybe {@code null} meaning no field sorting.
     */
    public Sort getSort() {
        return this.sort;
    }

    /**
     * Returns the Lucene {@link org.apache.lucene.search.Sort} represented by this {@link Sort} using the specified
     * {@link Schema}. Maybe {@code null} meaning no sorting.
     *
     * @param schema A {@link Schema}.
     * @return The Lucene {@link org.apache.lucene.search.Sort} represented by this {@link Sort} using {@code schema}.
     */
    public org.apache.lucene.search.Sort sort(Schema schema) {
        return sort == null ? null : sort.sort(schema);
    }

    /**
     * Returns the Lucene {@link Query} representation of this search. This {@link Query} include both the querying and
     * filtering {@link Condition}s. If none of them is set, then a {@link MatchAllDocsQuery} is returned, so it never
     * returns {@code null}.
     *
     * @param schema     The {@link Schema} to be used.
     * @param rangeQuery An additional range {@link Query} to be used.
     * @return The Lucene {@link Query} representation of this search.
     */
    public Query query(Schema schema, Query rangeQuery) {
        if (queryCondition == null && filterCondition == null && rangeQuery == null) {
            return new MatchAllDocsQuery();
        }
        BooleanQuery booleanQuery = new BooleanQuery();
        if (queryCondition != null) {
            Query query = queryCondition.query(schema);
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }
        if (filterCondition != null) {
            Query query = new ConstantScoreQuery(filterCondition.query(schema));
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }
        if (rangeQuery != null) {
            booleanQuery.add(rangeQuery, BooleanClause.Occur.MUST);
        }
        return booleanQuery;
    }

    /**
     * Validates this {@link Search} against the specified {@link Schema}.
     *
     * @param schema A {@link Schema}.
     */
    public void validate(Schema schema) {
        if (queryCondition != null || filterCondition != null) {
            query(schema, null);
        }
        if (sort != null) {
            sort.sort(schema);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                      .add("queryCondition", queryCondition)
                      .add("filterCondition", filterCondition)
                      .add("sort", sort)
                      .toString();
    }
}
