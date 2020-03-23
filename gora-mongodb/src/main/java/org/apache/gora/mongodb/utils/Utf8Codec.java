/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mongodb.utils;

import org.apache.avro.util.Utf8;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * BSON encoder for {@link Utf8} instances.
 */
public class Utf8Codec implements Codec<Utf8> {

    @Override
    public Utf8 decode(BsonReader reader, DecoderContext decoderContext) {
        return new Utf8(reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, Utf8 value, EncoderContext encoderContext) {
        writer.writeString(value.toString());
    }

    @Override
    public Class<Utf8> getEncoderClass() {
        return Utf8.class;
    }
}
