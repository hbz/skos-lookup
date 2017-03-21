import java.io.File;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import elasticsearch.ElasticsearchBuilder;

@SuppressWarnings("javadoc")
public class TestAutocomplete {
	ElasticsearchBuilder esb;
	String index = "agrovoc";

	@Before
	public void startUp() {
		esb = new ElasticsearchBuilder();

	}

	@Test
	public void testAutocomplete() {
		esb.init(index);
		esb.indexDirectory(new File("test/resources/testData"), index);
		SearchHits hits =
				esb.getInstance().autocompleteQuery(index, "Erdnus", "de", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(1 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
		hits = esb.getInstance().autocompleteQuery(index, "groundnuts", "en", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(1 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
	}

	// @Test
	public void testAutocompleteAll() {
		esb.init(index);
		esb.indexZippedFile(play.Environment.simple().resourceAsStream(
				"agrovoc_2016-07-15_lod.nt.gz"), index, RDFFormat.NTRIPLES);
		SearchHits hits =
				esb.getInstance().autocompleteQuery(index, "Erdnus", "de", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(4 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
		hits = esb.getInstance().autocompleteQuery(index, "groundnuts", "en", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(3 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
	}

	@After
	public void shutDown() {
		esb.getInstance().stopElasticSearch();
	}
}
