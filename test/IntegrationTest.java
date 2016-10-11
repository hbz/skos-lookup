import org.junit.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

@SuppressWarnings("javadoc")
public class IntegrationTest {

	/**
	 * add your integration test here in this example we just check if the welcome
	 * page is being shown
	 */
	@Test
	public void test() {
		running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT,
				browser -> {
					browser.goTo("http://localhost:3333/tools/skos-lookup/example");
					assertTrue(browser.pageSource().contains("Search"));
				});
	}

}
