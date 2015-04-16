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
package com.stratio.cassandra.index.schema.mapping;

import com.google.common.base.Objects;
import org.apache.cassandra.db.marshal.*;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A {@link ColumnMapper} to map a string, not tokenized field.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class ColumnMapperString extends ColumnMapperKeyword {

    /** The default case sensitive option. */
    public static final boolean DEFAULT_CASE_SENSITIVE = true;

    /** If it must be case sensitive. */
    private final boolean caseSensitive;

    /**
     * Builds a new {@link ColumnMapperString}.
     *
     * @param caseSensitive If the getAnalyzer must be case sensitive.
     */
    @JsonCreator
    public ColumnMapperString(@JsonProperty("case_sensitive") Boolean caseSensitive) {
        super(new AbstractType<?>[]{AsciiType.instance,
                                    UTF8Type.instance,
                                    Int32Type.instance,
                                    LongType.instance,
                                    IntegerType.instance,
                                    FloatType.instance,
                                    DoubleType.instance,
                                    BooleanType.instance,
                                    UUIDType.instance,
                                    TimeUUIDType.instance,
                                    TimestampType.instance,
                                    BytesType.instance,
                                    InetAddressType.instance}, new AbstractType[]{UTF8Type.instance});
        this.caseSensitive = caseSensitive == null ? DEFAULT_CASE_SENSITIVE : caseSensitive;
    }

    /**
     * Builds a new {@link ColumnMapperString} using {@link #DEFAULT_CASE_SENSITIVE}.
     */
    public ColumnMapperString() {
        this(DEFAULT_CASE_SENSITIVE);
    }

    /** {@inheritDoc} */
    @Override
    public String toLucene(String name, Object value, boolean checkValidity) {
        if (value == null) {
            return null;
        } else {
            String string = value.toString();
            return caseSensitive ? string : string.toLowerCase();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("caseSensitive", caseSensitive).toString();
    }
}
