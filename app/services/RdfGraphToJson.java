package services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;

/**
 * @author Jan Schnasse
 *
 */
public class RdfGraphToJson {

	/**
	 * @param in Input stream with rdf data
	 * @param format format of the rdf data
	 * @return a pretty JSON document as String
	 */
	public static String getPrettyJsonLdString(InputStream in, RDFFormat format) {
		return getPrettyJsonLdString(
				RdfUtils.readRdfToString(in, format, RDFFormat.JSONLD, ""));
	}

	/**
	 * @param statements rdf statements collected
	 * @return a pretty JSON document as String
	 */
	public static String getPrettyJsonLdString(Collection<Statement> statements) {
		return getPrettyJsonLdString(
				RdfUtils.graphToString(statements, RDFFormat.JSONLD));
	}

	private static String getPrettyJsonLdString(String rdfGraphAsJson) {
		try {
		//@formatter:off
				return JsonUtils
						.toPrettyString(
								getCompactedJson(
										createJsonObject(
												removeOuterBraces(
														rdfGraphAsJson))));
		//@formatter:on
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, Object> getCompactedJson(Object json) {
		try {
			return JsonLdProcessor.compact(json, getContext(), new JsonLdOptions());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getContext() {
		String jsonldContext = "skos-context.json";
		try (InputStream in =
				play.Environment.simple().resourceAsStream(jsonldContext)) {
			return (Map<String, Object>) JsonUtils.fromInputStream(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object createJsonObject(String ld) {
		try (InputStream inputStream =
				new ByteArrayInputStream(ld.getBytes(Charsets.UTF_8))) {
			Object jsonObject = JsonUtils.fromInputStream(inputStream);
			return jsonObject;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String removeOuterBraces(String ld) {
		return ld.substring(1, ld.length() - 1);
	}
}