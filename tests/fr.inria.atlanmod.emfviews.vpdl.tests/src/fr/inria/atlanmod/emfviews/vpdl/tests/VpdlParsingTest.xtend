/*
 * generated by Xtext 2.12.0
 */
package fr.inria.atlanmod.emfviews.vpdl.tests

import com.google.inject.Inject
import fr.inria.atlanmod.emfviews.vpdl.vpdl.View
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(VpdlInjectorProvider)
class VpdlParsingTest {
	@Inject
	ParseHelper<View> parseHelper
	
	@Test
	def void loadModel() {
		val result = parseHelper.parse('''
			Hello Xtext!
		''')
		Assert.assertNotNull(result)
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}
}