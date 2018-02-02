/*
 * generated by Xtext 2.12.0
 */
package fr.inria.atlanmod.emfviews.ide

import com.google.inject.Guice
import fr.inria.atlanmod.emfviews.MelRuntimeModule
import fr.inria.atlanmod.emfviews.MelStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class MelIdeSetup extends MelStandaloneSetup {

	override createInjector() {
		Guice.createInjector(Modules2.mixin(new MelRuntimeModule, new MelIdeModule))
	}
	
}