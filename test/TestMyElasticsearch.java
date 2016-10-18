
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import elasticsearch.MyElasticsearch;

/**
 * @author Jan Schnasse
 *
 */
@SuppressWarnings("javadoc")
public class TestMyElasticsearch {

	MyElasticsearch es = new MyElasticsearch();

	@SuppressWarnings("static-access")
	@Test
	public void indexZipFile() throws InterruptedException {
		String index = "ddc";
		es.init(index);
		es.indexZippedFile(play.Environment.simple().resourceAsStream("ddc.nt.gz"),
				index, RDFFormat.NTRIPLES);
		Thread.currentThread().sleep(3000);
		Assert.assertEquals(76408, es.getSize(index));
	}

	@After
	public void shutDown() {
		es.stopElasticSearch();
	}

}
