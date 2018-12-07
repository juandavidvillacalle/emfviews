/*
 * generated by Xtext 2.12.0
 */
package org.atlanmod.emfviews.vpdl.ide

import com.google.inject.Guice
import org.atlanmod.emfviews.vpdl.VpdlRuntimeModule
import org.atlanmod.emfviews.vpdl.VpdlStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class VpdlIdeSetup extends VpdlStandaloneSetup {

	override createInjector() {
		Guice.createInjector(Modules2.mixin(new VpdlRuntimeModule, new VpdlIdeModule))
	}

}