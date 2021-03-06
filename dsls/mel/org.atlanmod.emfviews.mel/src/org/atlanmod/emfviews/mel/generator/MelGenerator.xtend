/*******************************************************************************
 * Copyright (c) 2017, 2018 Armines
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3
 * which is available at https://www.gnu.org/licenses/gpl-3.0.txt
 *
 * Contributors:
 *   fmdkdd - initial API and implementation
 *******************************************************************************/

package org.atlanmod.emfviews.mel.generator

import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext
import org.atlanmod.emfviews.mel.mel.Model
import java.io.ByteArrayOutputStream
import org.atlanmod.emfviews.mel.mel.Metamodel
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.m2m.atl.emftvm.EmftvmFactory
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver

/*
 * Generates code from your model files on save.
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#code-generation
 */
class MelGenerator extends AbstractGenerator {

  def String extensionName(Resource r) {
    r.allContents.toIterable().filter(Model).<Model>head.extensionName
  }

  def Iterable<Metamodel> getAllMetamodels(Resource r) {
    r.allContents.toIterable().filter(Metamodel)
  }

  override void doGenerate(Resource resource, IFileSystemAccess2 fsa, IGeneratorContext context) {
    val name = extensionName(resource)

    fsa.generateFile(name + '.eviewpoint', resource.compileEviewpoint(fsa))
    fsa.generateFile(name + '.xmi', resource.compileXMI)
  }

  def CharSequence compileEviewpoint(Resource r, IFileSystemAccess2 fsa) '''
    contributingMetamodels=«r.getAllMetamodels.map([m | m.nsURI]).join(',')»
    weavingModel=«extensionName(r)».xmi
  '''

  def String compileXMI(Resource r) {
    val factory = EmftvmFactory.eINSTANCE
    val rs = new ResourceSetImpl()

    val env = factory.createExecEnv()

    // Load metamodels
    val sourceMM = factory.createMetamodel()
    sourceMM.resource = rs.getResource(URI.createURI("http://www.atlanmod.org/emfviews/mel/0.3.0"), true)
    env.registerMetaModel("MEL", sourceMM)

    val targetMM = factory.createMetamodel()
    targetMM.resource = rs.getResource(URI.createURI("http://www.atlanmod.org/emfviews/virtuallinks/0.3.0"), true)
    env.registerMetaModel("VirtualLinks", targetMM)

    // Load models
    val sourceModel = factory.createModel()
    sourceModel.resource = r
    env.registerInputModel("IN", sourceModel)

    val targetModel = factory.createModel()
    // The URI does not actually matter here, as we save the resource to a String
    targetModel.resource = rs.createResource(URI.createFileURI("foo.xmi"))
    env.registerOutputModel("OUT", targetModel)

    // Run the transformation
    val mr = new DefaultModuleResolver("platform:/plugin/org.atlanmod.emfviews.mel/transformation/",
      new ResourceSetImpl())

    env.loadModule(mr, "MEL2VirtualLinks")
    env.run(null)

    // Write to a String and return
    val out = new ByteArrayOutputStream()
    targetModel.resource.save(out, null)

    out.toString
  }
}
