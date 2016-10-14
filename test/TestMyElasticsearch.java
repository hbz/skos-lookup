
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import elasticsearch.MyElasticsearch;

/**
 * @author Jan Schnasse
 *
 */
public class TestMyElasticsearch {

	@Test
	public void indexZipFile() {
		MyElasticsearch es;
		String index = "agrovoc";
		es = new MyElasticsearch();
		es.init(index);
		es.indexZippedFile(play.Environment.simple().resourceAsStream(
				"agrovoc_2016-07-15_lod.nt.gz"), index, RDFFormat.NTRIPLES);
		Assert.assertEquals(32707, es.getSize(index));
	}

}
