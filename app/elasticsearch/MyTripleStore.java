package elasticsearch;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;
import org.eclipse.rdf4j.repository.event.base.NotifyingRepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.event.base.RepositoryConnectionListenerAdapter;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Jan Schnasse
 *
 */
public class MyTripleStore {
	Repository repo;

	/**
	 * Creates an inmemory triple store
	 * 
	 */
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

	/**
	 * @param in gzip compressed data on an inputstream
	 * @param format the format of the streamed data
	 */
	public void loadZippedFile(InputStream in, RDFFormat format) {
		play.Logger.info("Load zip file of format " + format);
		try (NotifyingRepositoryConnectionWrapper con =
				new NotifyingRepositoryConnectionWrapper(repo, repo.getConnection());
				InputStream gzipIn = new GZIPInputStream(in)) {
			RepositoryConnectionListenerAdapter myListener =
					new RepositoryConnectionListenerAdapter() {
						private long count = 0;

						@Override
						public void add(RepositoryConnection arg0, Resource arg1, IRI arg2,
								Value arg3, Resource... arg4) {
							count++;
							if (count % 100000 == 0)
								play.Logger.info("Add statement number " + count + "\n" + arg1
										+ " " + arg2 + " " + arg3);
						}
					};
			con.addRepositoryConnectionListener(myListener);
			con.add(gzipIn, "", format);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param in rdf data on an input stream
	 * @param format the rdf-format
	 */
	public void loadFile(InputStream in, RDFFormat format) {
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(in, "", format);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param concept an URI of a SKOS concept
	 * @return all statements with concept as rdf-subject
	 */
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
					Statement newS;
					if (o instanceof Literal) {
						Literal aLiteral = (Literal) o;
						newS = v.createStatement(v.createIRI(concept),
								v.createIRI(p.stringValue()), aLiteral);

					} else if (o instanceof BNode) {
						newS = v.createStatement(v.createIRI(concept),
								v.createIRI(p.stringValue()), v.createBNode(o.stringValue()));
					} else {
						newS = v.createStatement(v.createIRI(concept),
								v.createIRI(p.stringValue()), v.createIRI(o.stringValue()));
					}

					allStatementsOfOneConcept.add(newS);
				}
			}
			return allStatementsOfOneConcept;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return all concept URIs in the store
	 */
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
