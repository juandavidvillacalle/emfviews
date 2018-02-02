/*******************************************************************************
 * Copyright (c) 2013 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Juan David Villa Calle - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.emfviews.virtuallinksepsilondelegate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.ecl.EclModule;
import org.eclipse.epsilon.ecl.execute.EclOperationFactory;
import org.eclipse.epsilon.ecl.trace.Match;
import org.eclipse.epsilon.ecl.trace.MatchTrace;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import fr.inria.atlanmod.emfviews.virtuallinks.ConcreteConcept;
import fr.inria.atlanmod.emfviews.virtuallinks.ContributingModel;
import fr.inria.atlanmod.emfviews.virtuallinks.VirtualAssociation;
import fr.inria.atlanmod.emfviews.virtuallinks.VirtualLinksFactory;
import fr.inria.atlanmod.emfviews.virtuallinks.WeavingModel;
import fr.inria.atlanmod.emfviews.virtuallinks.delegator.IVirtualLinksDelegate;

public class EclDelegate implements IVirtualLinksDelegate {

  @Override
  public void createVirtualModelLinks(URI linksDslURI, URI linksModel,
                                      List<Resource> inputModelsResourcesList) throws Exception {

    File f;

    // Need to turn an EMF URI into an actual File location. We cannot use the URIConverter.INSTANCE since it only
    // provides InputStream, and EclModule needs an actual file.
    if (linksDslURI.isPlatform()) {
      // Find the system path for the file from the workspace URI
      IContainer wsroot = EcorePlugin.getWorkspaceRoot();
      IFile ifile = wsroot.getFile(new Path(linksDslURI.toPlatformString(true)));
      f = new File(ifile.getLocationURI());
    } else {
      // Assume a regular file path. Will throw if it cannot be found anyway.
      f = new File(linksDslURI.toFileString());
    }

    // Collect the contributing metamodels from the header of the ECL file
    // @Refactor: I'm not sure why this is even needed.
    // We could pass the metamodels directly as arguments to the ECL delegate.
    FileReader fr = new FileReader(f);
    BufferedReader br = new BufferedReader(fr);
    String sCurrentLine = "";

    Map<String, Resource> inputModelsAliasMapToResource = new HashMap<>();
    Map<String, String> inputmodelsAliasMapMetamodelUri = new HashMap<>();

    while (((sCurrentLine = br.readLine()) != null) && sCurrentLine.startsWith("//alias")) {
      String metamodelAlias =
          sCurrentLine.substring(sCurrentLine.indexOf("_") + 1, sCurrentLine.indexOf("="));
      String packageUri = sCurrentLine.substring(sCurrentLine.indexOf("=") + 1);

      Resource correctResource = null;
      boolean foundCorrectResource = false;
      for (int i = 0; i < inputModelsResourcesList.size() && !foundCorrectResource; i++) {
        Resource r = inputModelsResourcesList.get(i);
        EClassifier rootClassifier = r.getContents().get(0).eClass();
        // XXX: the profile.uml special case looks fishy
        if (rootClassifier.getEPackage().getNsURI().compareToIgnoreCase(packageUri) == 0
            && !r.getURI().toString().endsWith("profile.uml")) {
          correctResource = r;
          foundCorrectResource = true;
        }

      }
      // FIXME: what if correctResource is null?
      inputModelsAliasMapToResource.put(metamodelAlias, correctResource);

      inputmodelsAliasMapMetamodelUri.put(metamodelAlias, packageUri);

    }
    br.close();

    // Prepare the ECL Module

    // VirtualLinksPackage vl = VirtualLinksPackage.eINSTANCE;
    VirtualLinksFactory vLinksFactory = VirtualLinksFactory.eINSTANCE;
    WeavingModel weavingModel = vLinksFactory.createWeavingModel();

    XMIResourceImpl weavingModelResource = new XMIResourceImpl();

    weavingModelResource.setURI(linksModel);
    weavingModelResource.getContents().add(weavingModel);

    EclModule module = new EclModule();
    module.parse(f);
    if (module.getParseProblems().size() > 0) {
      System.err.println("Parse errors occured...");
      for (ParseProblem problem : module.getParseProblems()) {
        System.err.println(problem.toString());
      }
      throw new Exception("Error in parsing ECL file.  See stderr for details");
    }
    EclOperationFactory operationFactory = new EclOperationFactory();
    module.getContext().setOperationFactory(operationFactory);

    Iterator<Map.Entry<String, Resource>> iter =
        inputModelsAliasMapToResource.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, Resource> tempEntry = iter.next();
      Resource modelResource = tempEntry.getValue();
      EmfModel inputModel = null;
      if (!modelResource.getURI().toString().startsWith("cdo")) {
        inputModel = createEmfModelByURI(tempEntry.getKey(), modelResource.getURI().toString(),
                                         inputmodelsAliasMapMetamodelUri.get(tempEntry.getKey()),
                                         true, false);
      } else { // XXX: CDO special case seems fishy
        inputModel = new EmfModel();
        inputModel.setResource(modelResource);
        StringProperties properties = new StringProperties();
        properties.put(Model.PROPERTY_NAME, tempEntry.getKey());
        properties.put(EmfModel.PROPERTY_METAMODEL_URI,
                       inputmodelsAliasMapMetamodelUri.get(tempEntry.getKey()));
        properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "false");
        inputModel.load(properties);
        inputModel.setCachingEnabled(true);
        TreeIterator<EObject> modelContents = modelResource.getAllContents();
        ArrayList<EObject> allModelContents = new ArrayList<>();
        while (modelContents.hasNext()) {
          allModelContents.add(modelContents.next());

        }
        inputModel.allContents().addAll(allModelContents);

      }
      module.getContext().getModelRepository().addModel(inputModel);
    }

    MatchTrace mt = (MatchTrace) module.execute();

    List<Match> matches = mt.getMatches();

    HashMap<String, ContributingModel> modelsByURI = new HashMap<>();

    for (Match match : matches) {
      if (match.isMatching()) {
        EObject left = (EObject) match.getLeft();
        EObject right = (EObject) match.getRight();

        VirtualAssociation vAsso = vLinksFactory.createVirtualAssociation();
        vAsso.setName(match.getRule().getName());
        vAsso.setLowerBound(0);
        vAsso.setUpperBound(1);

        ConcreteConcept lSource = vLinksFactory.createConcreteConcept();
        lSource.setPath(left.eResource().getURIFragment(left));

        String sourceModelURI = left.eClass().getEPackage().getNsURI();
        if (!modelsByURI.containsKey(sourceModelURI)) {
          ContributingModel m = vLinksFactory.createContributingModel();
          m.setURI(sourceModelURI);
          modelsByURI.put(sourceModelURI, m);
          weavingModel.getContributingModels().add(m);
        }
        lSource.setModel(modelsByURI.get(sourceModelURI));

        vAsso.setSource(lSource);

        ConcreteConcept lTarget = vLinksFactory.createConcreteConcept();
        lTarget.setPath(right.eResource().getURIFragment(right));
        // TODO: check the linked elements are concepts

        String targetModelURI = right.eClass().getEPackage().getNsURI();
        if (!modelsByURI.containsKey(targetModelURI)) {
          ContributingModel m = vLinksFactory.createContributingModel();
          m.setURI(targetModelURI);
          modelsByURI.put(targetModelURI, m);
          weavingModel.getContributingModels().add(m);
        }
        lTarget.setModel(modelsByURI.get(targetModelURI));

        vAsso.setTarget(lTarget);

        weavingModel.getVirtualLinks().add(vAsso);
      }
    }
    weavingModelResource.setURI(linksModel);
    weavingModelResource.getContents().add(weavingModel);
    weavingModelResource.save(null);
  }

  protected EmfModel createEmfModelByURI(String name, String model, String metamodel,
                                         boolean readOnLoad,
                                         boolean storeOnDisposal) throws EolModelLoadingException {
    // @Correctness this assumes the metamodels are in the EPackage.Registry.
    // This does not hold for using Ecore files directly, unless we load them into
    // the registry first.
    // See if we can tell Epsilon to fallback on the Ecore file.

    // @Correctness this condition seems fishy
    if (metamodel.contains("UML")) {
      UMLResourcesUtil.init(null);
    }

    EmfModel emfModel = new EmfModel();
    StringProperties properties = new StringProperties();
    properties.put(Model.PROPERTY_NAME, name);
    properties.put(EmfModel.PROPERTY_METAMODEL_URI, metamodel);
    properties.put(EmfModel.PROPERTY_MODEL_URI, model);
    properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "false");
    properties.put(Model.PROPERTY_READONLOAD, readOnLoad + "");
    properties.put(Model.PROPERTY_STOREONDISPOSAL, storeOnDisposal + "");
    emfModel.load(properties);
    return emfModel;
  }

}
