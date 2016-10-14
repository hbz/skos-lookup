package elasticsearch;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Jan Schnasse
 *
 */
public class MyTripleStore {
	Repository repo;

	public MyTripleStore() {
		repo = new SailRepository(new MemoryStore());
		repo.initialize();
	}

	/**
	 * @param dir if a dir is provided. The repo will remember its data.
	 */
	public MyTripleStore(String dir) {
		File dataDir = new File(dir);
		repo = new SailRepository(new MemoryStore(dataDir));
		repo.initialize();
	}

	public void loadZippedFile(InputStream in, RDFFormat format) {
		play.Logger.info("Load zip file of format " + format);
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(new GZIPInputStream(in), "", format);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void loadFile(InputStream in, RDFFormat format) {
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(in, "", format);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<Statement> getConcept(String concept) {
		try (RepositoryConnection con = repo.getConnection()) {
			ValueFactory v = SimpleValueFactory.getInstance();
			Collection<Statement> allStatementsOfOneConcept = new ArrayList<>();
			String queryString = "SELECT ?p ?o { <" + concept + "> ?p ?o . } ";
			TupleQuery tupleQuery =
					con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Value p = bindingSet.getValue("p");
					Value o = bindingSet.getValue("o");
					Statement newS = v.createStatement(v.createIRI(concept),
							v.createIRI(p.stringValue()), v.createLiteral(o.stringValue()));
					allStatementsOfOneConcept.add(newS);
				}
			}
			return allStatementsOfOneConcept;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Set<String> getAllConcepts() {
		play.Logger.info("Query for all concepts!");
		try (RepositoryConnection con = repo.getConnection()) {
			Set<String> concepts = new HashSet<>();
			String queryString =
					"SELECT ?s { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"
							+ SKOS.Concept + "> } ";
			TupleQuery tupleQuery =
					con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Value valueOfX = bindingSet.getValue("s");
					concepts.add(valueOfX.stringValue());
				}
			}
			return concepts;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
