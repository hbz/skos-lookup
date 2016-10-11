import java.io.File;

import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import elasticsearch.MyElasticsearch;

@SuppressWarnings("javadoc")
public class ElasticsearchTest {
	MyElasticsearch es;
	String index = "agrovoc";

	@Before
	public void startUp() {
		es = new MyElasticsearch();
		es.init(index);
		es.indexDirectory(new File("test/resources/testData"), index);
	}

	@Test
	public void testAutocomplete() {
		SearchHits hits = es.autocompleteQuery(index, "Erdnuss", "de", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(1 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
		hits = es.autocompleteQuery(index, "groundnuts", "en", 0, 10);
		play.Logger.debug("Total Hits " + hits.totalHits());
		play.Logger.debug("HIT " + hits.getHits()[0].getSource().get("id"));
		Assert.assertTrue(1 == hits.totalHits());
		Assert.assertTrue("http://aims.fao.org/aos/agrovoc/c_11368"
				.equals(hits.getHits()[0].getSource().get("id")));
	}

	@After
	public void shutDown() {
		es.stopElasticSearch();
	}
}
