
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import elasticsearch.ElasticsearchBuilder;

/**
 * @author Jan Schnasse
 *
 */
@SuppressWarnings("javadoc")
public class TestMyElasticsearch {

	ElasticsearchBuilder esb = new ElasticsearchBuilder();

	@SuppressWarnings("static-access")
	@Test
	public void indexZipFile() throws InterruptedException {
		String index = "ddc";
		esb.init(index);
		esb.indexFile(play.Environment.simple().resourceAsStream("ddc.nt.gz"),
				index, RDFFormat.NTRIPLES);
		Thread.currentThread().sleep(3000);
		Assert.assertEquals(76408, esb.getInstance().getSize(index));
	}

	@After
	public void shutDown() {
		esb.getInstance().stopElasticSearch();
	}

}
