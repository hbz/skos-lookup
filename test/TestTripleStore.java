
import java.util.Collection;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import elasticsearch.MyTripleStore;
import services.RdfUtils;

/**
 * @author Jan Schnasse
 *
 */

@SuppressWarnings("javadoc")
public class TestTripleStore {

	@Test
	public void test_small() {
		MyTripleStore ts = new MyTripleStore();
		ts.loadFile(play.Environment.simple().resourceAsStream("testData/1000.nt"),
				RDFFormat.NTRIPLES);
		Assert.assertEquals(1, ts.getAllConcepts().size());
	}

	// @Test
	public void test_large_agrovoc() {
		MyTripleStore ts = new MyTripleStore();
		ts.loadZippedFile(play.Environment.simple()
				.resourceAsStream("agrovoc_2016-07-15_lod.nt.gz"), RDFFormat.NTRIPLES);
		Assert.assertEquals(32707, ts.getAllConcepts().size());
	}

	@Test
	public void test_large_ddc() {
		MyTripleStore ts = new MyTripleStore();
		ts.loadZippedFile(play.Environment.simple().resourceAsStream("ddc.nt.gz"),
				RDFFormat.NTRIPLES);
		Assert.assertEquals(76408, ts.getAllConcepts().size());
	}

	// @Test
	public void test_very_large() {
		MyTripleStore ts = new MyTripleStore();
		ts.loadZippedFile(play.Environment.simple()
				.resourceAsStream("agrovoc_2016-07-15_lod.nt.gz"), RDFFormat.NTRIPLES);
		ts.loadZippedFile(play.Environment.simple().resourceAsStream("ddc.nt.gz"),
				RDFFormat.NTRIPLES);
		ts.loadFile(play.Environment.simple().resourceAsStream("testData/1000.nt"),
				RDFFormat.NTRIPLES);
		Assert.assertEquals(109115, ts.getAllConcepts().size());
	}

	@Test
	public void test_small_statements() {
		MyTripleStore ts = new MyTripleStore();
		ts.loadFile(play.Environment.simple().resourceAsStream("testData/1000.nt"),
				RDFFormat.NTRIPLES);
		Assert.assertEquals(1, ts.getAllConcepts().size());
		Collection<Statement> statements =
				ts.getConcept(ts.getAllConcepts().iterator().next());
		String asTurtle = RdfUtils.graphToString(statements, RDFFormat.TURTLE);
		System.out.println(asTurtle);
	}
}
