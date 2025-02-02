/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.document.id.IdGenerator;
import org.springframework.ai.document.id.RandomIdGenerator;
import org.springframework.ai.model.Media;
import org.springframework.ai.model.MediaContent;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A document is a container for the content and metadata of a document. It also contains
 * the document's unique ID and an optional embedding.
 */
@JsonIgnoreProperties({ "contentFormatter" })
public class Document implements MediaContent {

	public final static ContentFormatter DEFAULT_CONTENT_FORMATTER = DefaultContentFormatter.defaultConfig();

	public final static String EMPTY_TEXT = "";

	/**
	 * Unique ID
	 */
	private final String id;

	/**
	 * Document content.
	 */
	private final String content;

	private final Collection<Media> media;

	/**
	 * Metadata for the document. It should not be nested and values should be restricted
	 * to string, int, float, boolean for simple use with Vector Dbs.
	 */
	private Map<String, Object> metadata;

	/**
	 * Embedding of the document. Note: ephemeral field.
	 */
	@JsonProperty(index = 100)
	private float[] embedding = new float[0];

	/**
	 * Mutable, ephemeral, content to text formatter. Defaults to Document text.
	 */
	@JsonIgnore
	private ContentFormatter contentFormatter = DEFAULT_CONTENT_FORMATTER;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public Document(@JsonProperty("content") String content) {
		this(content, new HashMap<>());
	}

	public Document(String content, Map<String, Object> metadata) {
		this(content, metadata, new RandomIdGenerator());
	}

	public Document(String content, Collection<Media> media, Map<String, Object> metadata) {
		this(new RandomIdGenerator().generateId(content, metadata), content, media, metadata);
	}

	public Document(String content, Map<String, Object> metadata, IdGenerator idGenerator) {
		this(idGenerator.generateId(content, metadata), content, metadata);
	}

	public Document(String id, String content, Map<String, Object> metadata) {
		this(id, content, List.of(), metadata);
	}

	public Document(String id, String content, Collection<Media> media, Map<String, Object> metadata) {
		Assert.hasText(id, "id must not be null or empty");
		Assert.notNull(content, "content must not be null");
		Assert.notNull(metadata, "metadata must not be null");

		this.id = id;
		this.content = content;
		this.media = media;
		this.metadata = metadata;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String getContent() {
		return this.content;
	}

	@Override
	public Collection<Media> getMedia() {
		return this.media;
	}

	@JsonIgnore
	public String getFormattedContent() {
		return this.getFormattedContent(MetadataMode.ALL);
	}

	public String getFormattedContent(MetadataMode metadataMode) {
		Assert.notNull(metadataMode, "Metadata mode must not be null");
		return this.contentFormatter.format(this, metadataMode);
	}

	/**
	 * Helper content extractor that uses and external {@link ContentFormatter}.
	 */
	public String getFormattedContent(ContentFormatter formatter, MetadataMode metadataMode) {
		Assert.notNull(formatter, "formatter must not be null");
		Assert.notNull(metadataMode, "Metadata mode must not be null");
		return formatter.format(this, metadataMode);
	}

	@Override
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	/**
	 * Return the embedding that were calculated.
	 * @deprecated We are considering getting rid of this, please comment on
	 * https://github.com/spring-projects/spring-ai/issues/1781
	 * @return the embeddings
	 */
	@Deprecated(since = "1.0.0-M4")
	public float[] getEmbedding() {
		return this.embedding;
	}

	public void setEmbedding(float[] embedding) {
		Assert.notNull(embedding, "embedding must not be null");
		this.embedding = embedding;
	}

	/**
	 * Returns the content formatter associated with this document.
	 * @deprecated We are considering getting rid of this, please comment on
	 * https://github.com/spring-projects/spring-ai/issues/1782
	 * @return the current ContentFormatter instance used for formatting the document
	 * content.
	 */
	public ContentFormatter getContentFormatter() {
		return this.contentFormatter;
	}

	/**
	 * Replace the document's {@link ContentFormatter}.
	 * @param contentFormatter new formatter to use.
	 */
	public void setContentFormatter(ContentFormatter contentFormatter) {
		this.contentFormatter = contentFormatter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		result = prime * result + ((this.metadata == null) ? 0 : this.metadata.hashCode());
		result = prime * result + ((this.content == null) ? 0 : this.content.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Document other = (Document) obj;
		if (this.id == null) {
			if (other.id != null) {
				return false;
			}
		}
		else if (!this.id.equals(other.id)) {
			return false;
		}
		if (this.metadata == null) {
			if (other.metadata != null) {
				return false;
			}
		}
		else if (!this.metadata.equals(other.metadata)) {
			return false;
		}
		if (this.content == null) {
			if (other.content != null) {
				return false;
			}
		}
		else if (!this.content.equals(other.content)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Document{" + "id='" + this.id + '\'' + ", metadata=" + this.metadata + ", content='" + this.content
				+ '\'' + ", media=" + this.media + '}';
	}

	public static class Builder {

		private String id;

		private String content = Document.EMPTY_TEXT;

		private List<Media> media = new ArrayList<>();

		private Map<String, Object> metadata = new HashMap<>();

		private IdGenerator idGenerator = new RandomIdGenerator();

		public Builder withIdGenerator(IdGenerator idGenerator) {
			Assert.notNull(idGenerator, "idGenerator must not be null");
			this.idGenerator = idGenerator;
			return this;
		}

		public Builder withId(String id) {
			Assert.hasText(id, "id must not be null or empty");
			this.id = id;
			return this;
		}

		public Builder withContent(String content) {
			Assert.notNull(content, "content must not be null");
			this.content = content;
			return this;
		}

		public Builder withMedia(List<Media> media) {
			Assert.notNull(media, "media must not be null");
			this.media = media;
			return this;
		}

		public Builder withMedia(Media media) {
			Assert.notNull(media, "media must not be null");
			this.media.add(media);
			return this;
		}

		public Builder withMetadata(Map<String, Object> metadata) {
			Assert.notNull(metadata, "metadata must not be null");
			this.metadata = metadata;
			return this;
		}

		public Builder withMetadata(String key, Object value) {
			Assert.notNull(key, "key must not be null");
			Assert.notNull(value, "value must not be null");
			this.metadata.put(key, value);
			return this;
		}

		public Document build() {
			if (!StringUtils.hasText(this.id)) {
				this.id = this.idGenerator.generateId(this.content, this.metadata);
			}
			return new Document(this.id, this.content, this.media, this.metadata);
		}

	}

}
