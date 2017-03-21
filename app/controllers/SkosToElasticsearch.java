/*Copyright (c) 2016 "hbz"

This file is part of skos-lookup.

skos-lookup is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import elasticsearch.ElasticsearchBuilder;
import play.data.DynamicForm;
import play.data.Form;
import play.inject.ApplicationLifecycle;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import views.html.autocomplete;
import views.html.upload;

/**
 * @author Jan Schnasse
 *
 */
public class SkosToElasticsearch extends Controller {

	/**
	 * Elasticsearchinterface
	 */
	public final static ElasticsearchBuilder esb = new ElasticsearchBuilder();

	/**
	 * @param lifecycle parameter to hook into application management
	 */
	@Inject
	public SkosToElasticsearch(ApplicationLifecycle lifecycle) {
		lifecycle.addStopHook(() -> {
			esb.getInstance().stopElasticSearch();
			return CompletableFuture.completedFuture(null);
		});
	}

	/**
	 * Requires http-post data with -Fdata=binary file data -Findex=name of the
	 * index -Fcompression=<raw|gzip> -Fformat=<TURTLE|NTRIPLES|RDFXML>
	 * 
	 * @return http 200 if everything has been imported
	 */
	public CompletionStage<Result> init() {
		CompletableFuture<Result> future = new CompletableFuture<>();
		try {
			@SuppressWarnings("rawtypes")
			MultipartFormData body = request().body().asMultipartFormData();
			DynamicForm requestData = Form.form().bindFromRequest();
			String format = requestData.get("format");
			play.Logger.debug(format);
			@SuppressWarnings("rawtypes")
			FilePart data = body.getFile("data");
			String index = requestData.get("index");
			String compression = requestData.get("compression");
			if (data != null) {
				uploadData(future, format, data, index, compression);
			} else {
				flash("error", "Missing file");
				future.complete(redirect("/example"));
			}
			return future;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void uploadData(CompletableFuture<Result> future, String format,
			FilePart data, String index, String compression)
			throws IOException, FileNotFoundException {
		File file = (File) data.getFile();
		try (FileInputStream uploadData = new FileInputStream(file)) {
			RDFFormat f = initalizeRdfFormat(format);
			initalizeBuilder(index, compression, uploadData, f);
			flash("info", "File uploaded");
			future.complete(ok());
		}
	}

	private RDFFormat initalizeRdfFormat(String format) {
		RDFFormat f = RDFFormat.RDFXML;
		if ("TURTLE".equals(format)) {
			f = RDFFormat.TURTLE;
		} else if ("NTRIPLES".equals(format)) {
			f = RDFFormat.NTRIPLES;
		} else if ("RDFXML".equals(format)) {
			f = RDFFormat.RDFXML;
		}
		return f;
	}

	private void initalizeBuilder(String index, String compression,
			FileInputStream uploadData, RDFFormat f) {
		esb.init(index);
		if ("gzip".equals(compression)) {
			esb.indexZippedFile(uploadData, index, f);
		} else {
			esb.indexFile(uploadData, index, f);
		}
	}

	/**
	 * @return a html form with an example on how to use the autocompletion
	 *         endpoint
	 */
	public CompletionStage<Result> autocompleteExample() {
		CompletableFuture<Result> future = new CompletableFuture<>();
		Config conf = ConfigFactory.load();
		future.complete(
				ok(autocomplete.render(esb.getInstance().getIndexList(), conf)));
		return future;

	}

	/**
	 * @param q performs a matchQuery against prefLabel.{@paramref lang}
	 * @param lang a language for autocomplete
	 * @param index the index to get autocomplete suggestions from
	 * @return a list of (label, value)-pairs wrapped in a jsonp callback. If no
	 *         callback has been passed, the list is returned anyway
	 */
	public CompletionStage<Result> autocomplete(String q, String lang,
			String index) {
		CompletableFuture<Result> future = new CompletableFuture<>();
		final String[] callback =
				request() == null || request().queryString() == null ? null
						: request().queryString().get("callback");
		SearchHits hits =
				esb.getInstance().autocompleteQuery(index, q, lang, 0, 10);
		List<Map<String, String>> result = new ArrayList<>();
		hits.forEach((hit) -> {
			Map<String, Object> h = hit.getSource();
			String label = getLabel(h, lang);
			String id = getId(h);
			Map<String, String> m = new HashMap<>();
			m.put("label", label);
			m.put("value", id);
			result.add(m);
		});
		String searchResult = SkosToElasticsearch.json(result);
		String response = callback != null
				? String.format("/**/%s(%s)", callback[0], searchResult) : searchResult;
		future.complete(ok(response));
		return future;
	}

	private static String getId(Map<String, Object> h) {

		return (String) h.get("id");
	}

	private static String getLabel(Map<String, Object> h, String lang) {
		@SuppressWarnings("unchecked")
		Map<String, Object> labelMap = (Map<String, Object>) h.get("prefLabel");
		return (String) labelMap.get(lang);
	}

	/**
	 * @param q the q string will passed to elasticsearch as a queryStringQuery
	 * @param index the index to search in
	 * @return an array of documents
	 */
	public CompletionStage<Result> search(String q, String index) {
		response().setHeader("content-type", "application/json");
		String escaped_q = QueryParserBase.escape(q);
		CompletableFuture<Result> future = new CompletableFuture<>();
		SearchHits hits = esb.getInstance().query(index, escaped_q, 0, 10);
		List<SearchHit> list = Arrays.asList(hits.getHits());
		List<Map<String, Object>> hitMap = new ArrayList<>();
		for (SearchHit hit : list) {
			Map<String, Object> m = hit.getSource();
			m.put("primaryTopic", hit.getId());
			hitMap.add(m);
		}
		future.complete(ok(SkosToElasticsearch.json(hitMap)));
		return future;

	}

	private static String json(Object obj) {
		try {
			StringWriter w = new StringWriter();
			new ObjectMapper().writeValue(w, obj);
			String result = w.toString();
			return result;
		} catch (Exception e) {
			play.Logger.error("", e);
			return "{\"message\":\"error\"}";
		}
	}

	/**
	 * @return a simple upload form for rdf files
	 */
	public CompletionStage<Result> upload() {
		CompletableFuture<Result> future = new CompletableFuture<>();
		Config conf = ConfigFactory.load();
		future.complete(ok(upload.render(esb.getInstance().getIndexList(), conf)));
		return future;
	}
}
