/*******************************************************************************
 * Copyright (c) 2017--2019 Armines
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

package org.atlanmod.emfviews.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.atlanmod.emfviews.core.View;
import org.atlanmod.emfviews.core.ViewResource;
import org.atlanmod.emfviews.core.Viewpoint;
import org.atlanmod.emfviews.core.ViewpointResource;
import org.atlanmod.emfviews.virtuallinks.VirtualLinksFactory;
import org.atlanmod.emfviews.virtuallinks.VirtualLinksPackage;
import org.atlanmod.emfviews.virtuallinks.WeavingModel;
import org.atlanmod.emfviews.virtuallinks.delegator.VirtualLinksDelegator;
import org.atlanmod.emfviews.virtuallinksepsilondelegate.EclDelegate;
import org.atlanmod.sexp2emf.Sexp2EMF;

// Fix the run order since Eclipse is incapable of doing that for the output.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestEMFViews {

  @BeforeClass
  public static void setup() {
    EcorePackage.eINSTANCE.eClass();
    VirtualLinksPackage.eINSTANCE.eClass();
  }

  @Test
  public void threeModelComposition() throws IOException {
    // Based on the EA_viewtest examples, this is an integration test combining
    // three metamodels (contentfwk, BPMN2 and reqif10), with filters and
    // associations.

    // First check the metamodels (Viewpoint)
    {
      ViewpointResource v = new ViewpointResource(resourceURI("viewpoints/three-model-composition/viewpoint.eviewpoint"));
      v.load(null);
      assertNoErrors(v);

      // We have access to all the contents
      TreeIterator<EObject> it = v.getAllContents();
      while (it.hasNext()) {
        it.next();
      }

      // After this point, ideally we should just compare the Viewpoint
      // with a serialized "expected" result.  But this is too unwieldy
      // for large models.
      EList<EObject> l = v.getContents().get(0).eContents();

      // Ensure we have the three packages we want
      assertEquals(3, l.size());
      List<String> names = l.stream().map(o -> (String) eGet(o, "name")).collect(Collectors.toList());
      assertTrue(names.containsAll(Arrays.asList("contentfwk", "bpmn2", "reqif10")));

      // Find contentfwk package
      EObject cfwk = l.stream().filter(o -> "contentfwk".equals(eGet(o, "name"))).findFirst().get();

      // Ensure the filtered elements are absent
      EObject c = getClassifier(cfwk, "BusinessArchitecture").get();
      assertEquals(1, getFeatures(c).size());
      // and make sure the feature we left is in there
      assertTrue(getFeature(c, "processes").isPresent());

      // The original model is *not* modified
      c = v.getViewpoint().getContributingEPackages().get("cfw").getEClassifier("BusinessArchitecture");
      assertEquals(16, getFeatures(c).size());

      // Ensure our virtual associations are in there
      EObject p = getClassifier(cfwk, "Process").get();
      assertTrue(getFeature(p, "detailedProcess").isPresent());

      EObject r = getClassifier(cfwk, "Requirement").get();
      assertTrue(getFeature(r, "detailedRequirement").isPresent());
    }

    // Then, do the same for models (EView)
    {
      ViewResource v = new ViewResource(resourceURI("views/three-model-composition/view.eview"));
      v.load(null);

      // We have access to all the contents
      TreeIterator<EObject> it = v.getAllContents();
      while (it.hasNext()) {
        it.next();
      }

      EList<EObject> l = v.getContents();

      assertEquals(3, l.size());
      List<String> names = l.stream().map(o -> o.eClass().getName()).collect(Collectors.toList());
      assertTrue(names.containsAll(Arrays.asList("EnterpriseArchitecture", "ReqIF", "Definitions")));

      // Find the Business Architecture instance
      EObject ea = l.stream().filter(o -> "EnterpriseArchitecture".equals(o.eClass().getName())).findFirst().get();
      EObject ba = ea.eContents().get(1);

      // Check we only have "Process" instances in there (others are filtered
      // out), and that they each have a detailedProcess feature
      assertFalse(ba.eContents().isEmpty());
      for (EObject e : ba.eContents()) {
        assertEquals("Process", e.eClass().getName());
        assertNotNull(e.eClass().getEStructuralFeature("detailedProcess"));
      }

      // Furthermore, the detailed process for "Booking a trip" should link to a
      // BPMN Process
      EObject p = ba.eContents().get(0);
      EObject dp = (EObject) eGet(p, "detailedProcess");
      assertNotNull(dp);
      assertEquals("bpmn2", eGet(dp.eClass().eContainer(), "name"));
    }
  }

  @Test
  public void viewMaintenanceAttribute() throws IOException {
    // If a contributing model attribute is updated, the change is reflected in the view

    View v = loadView("views/minimal-assoc/view.eview");
    EList<EObject> l = v.getVirtualContents();
    EObject vB = l.get(1);
    Resource BM = v.getContributingModels().get(1);
    EObject B = BM.getContents().get(0);

    assertEquals(42, eGet(B, "b"));
    assertEquals(42, eGet(vB, "b"));

    eSet(B, "b", 2);

    assertEquals(2, eGet(B, "b"));
    assertEquals(2, eGet(vB, "b"));
  }

  @Test
  public void viewMaintenanceReference() {
    // If an element is removed from a contributing model reference,
    // the change is reflected in the view

    // Construct the viewpoint
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A'"
        + "                       :eStructuralFeatures [(EReference :name 'manyB'"
        + "                                                         :upperBound -1"
        + "                                                         :eType @B)])"
        + "               #B(EClass :name 'B')])",
        EcoreFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint viewpoint = new Viewpoint(m);

    // Construct the view
    EObject[] model = Sexp2EMF.build("[(A :manyB [@B1 @B2])"
                                   + " #B1(B) #B2(B)]",
                                   P.getEFactoryInstance());
    Resource r = new ResourceImpl();
    r.getContents().addAll(Arrays.asList(model));
    View view = new View(viewpoint, Arrays.asList(r));

    EObject A = model[0];
    EObject B1 = model[1];
    EObject B2 = model[2];
    EObject vA = view.getVirtualContents().get(0);
    EObject vB1 = view.getVirtualContents().get(1);
    EObject vB2 = view.getVirtualContents().get(2);

    assertEquals(Arrays.asList(B1, B2), eGet(A, "manyB"));
    assertEquals(Arrays.asList(vB1, vB2), eGet(vA, "manyB"));

    @SuppressWarnings("unchecked")
    EList<EObject> l = (EList<EObject>) eGet(A, "manyB");
    l.remove(1);

    assertEquals(Arrays.asList(B1), eGet(A, "manyB"));
    assertEquals(Arrays.asList(vB1), eGet(vA, "manyB"));
  }

  @Test
  public void accessToFilteredFeature() throws IOException {
    // We should not be able to access a filtered feature in any way.

    // Get the view
    View v = loadView("views/three-model-composition/view.eview");

    EList<EObject> l = v.getVirtualContents();

    // Find the Business Architecture instance where there are filtered elements
    EObject vcfwk = l.stream().filter(o -> "EnterpriseArchitecture".equals(o.eClass().getName())).findFirst().get();
    EObject vba = vcfwk.eContents().get(1);

    // Make sure filtered features are absent
    assertFalse(vba.eContents().isEmpty());
    for (EObject e : vba.eContents()) {
      assertEquals("Process", e.eClass().getName());
    }

    // If we have the original feature from the unfiltered model
    Resource m = v.getContributingModels().stream().filter(o -> "contentfwk".equals(o.getContents().get(0).eClass().getEPackage().getName()))
      .findFirst().get();
    EObject ba = m.getContents().get(0).eContents().get(1);
    EStructuralFeature f = ba.eClass().getEStructuralFeature("drivers");

    // Make sure we cannot access it that way
    try {
      vba.eGet(f);
      fail("Expected the feature to not be found");
    } catch (IllegalArgumentException ex) {
      assertEquals("The feature 'drivers' is not a valid feature", ex.getMessage());
    }
  }

  @Test
  public void filterBidirectionalReference() throws IOException {
    // When we filter a reference that has an opposite, its opposite still
    // exists, but its EOpposite link should be null.

    // Create the view
    View v = loadView("views/minimal/view.eview");

    // The model has a many ref from A to B, and a single ref from B to A, but
    // the metamodel has filtered the ref in A.
    EList<EObject> l = v.getVirtualContents();
    EObject a = l.get(0);

    // Make sure the ref in A is filtered
    assertEquals("A", a.eClass().getName());
    assertNull(a.eClass().getEStructuralFeature("manyB"));

    // The opposite feature should still exist on the metamodel for B
    EObject b = l.get(1);
    EObject parentA = b.eClass().getEStructuralFeature("parentA");
    assertNotNull(parentA);

    // Make sure we cannot access the feature through its opposite
    assertNull(eGet(parentA, "eOpposite"));
  }

  @Test
  public void virtualAssociation() throws IOException {
    // Creating a virtual association between two minimal models.

    // Create the view
    View v = loadView("views/minimal-assoc/view.eview");

    // Get the virtual model
    EList<EObject> l = v.getVirtualContents();

    // Check it contains attributes from the source models
    EObject A = l.get(0);
    EObject B = l.get(1);
    assertEquals(true, eGet(A, "a"));
    assertEquals(42, eGet(B, "b"));

    // Check it contains the virtual association
    assertEquals(B, eGet(A, "assoc"));
  }

  @Test
  public void concreteAssociation() {
    // An existing association is still reachable in a viewpoint

    // Create the viewpoint
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A'"
        + "                :eStructuralFeatures [(EReference :name 'refToB'"
        + "                                       :eType @1)])"
        + "               #1(EClass :name 'B')])",
        EcoreFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint v = new Viewpoint(m);

    // The association in the viewpoint is consistent with the contributing metamodel
    EPackage VP = v.getRootPackage().getESubpackages().get(0);
    EReference refToB = (EReference) ((EClass) VP.getEClassifier("A")).getEStructuralFeature("refToB");
    assertEquals(VP.getEClassifier("B"), refToB.getEType());
  }

  @Test
  public void addConcept() throws IOException {
    // A new concept in the weaving model should be added to the virtual package

    Viewpoint v = loadViewpoint("viewpoints/addconcept/viewpoint.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(1);
    // The virtual package takes the WeavingModel name
    assertEquals("addconcept", eGet(p, "name"));
    // And it holds our new concept
    assertTrue(getClassifier(p, "C").isPresent());
  }

  @Test
  public void addSubConcept() throws IOException {
    // A new subconcept in the weaving model should be added to the virtual
    // package, and should reference its superconcept.

    Viewpoint v = loadViewpoint("viewpoints/addconcept/subconcept.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(2);
    // The virtual package takes the WeavingModel name
    assertEquals("subconcept", eGet(p, "name"));
    // It holds our new concept
    EObject C = getClassifier(p, "C").get();
    // C has A and B as super types
    EList<EObject> sups = eList(C, "eSuperTypes");
    assertEquals(2, sups.size());
    assertEquals(getClassifier(l.get(0), "A").get(), sups.get(0));
    assertEquals(getClassifier(l.get(1), "B").get(), sups.get(1));
  }

  @Test
  public void addSuperConcept() throws IOException {
    // A new subconcept in the weaving model should be added to the virtual
    // package, and should reference its superconcept.

    Viewpoint v = loadViewpoint("viewpoints/addconcept/superconcept.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(2);
    // The virtual package takes the WeavingModel name
    assertEquals("superconcept", eGet(p, "name"));
    // It holds our new concept
    EObject C = getClassifier(p, "C").get();
    // A and B both have C as super type
    {
      EList<EObject> sups = eList(getClassifier(l.get(0), "A").get(), "eSuperTypes");
      assertEquals(1, sups.size());
      assertEquals(C, sups.get(0));
    }
    {
      EList<EObject> sups = eList(getClassifier(l.get(1), "B").get(), "eSuperTypes");
      assertEquals(1, sups.size());
      assertEquals(C, sups.get(0));
    }

    // The original metamodels are *not* modified
    assertEquals(0, eList(getClassifier(v.getContributingEPackages().get("A"), "A").get(), "eSuperTypes").size());
    assertEquals(0, eList(getClassifier(v.getContributingEPackages().get("B"), "B").get(), "eSuperTypes").size());
  }

  @Test
  public void addProperty() throws IOException {
    // A new property should be added to its target concept.

    Viewpoint v = loadViewpoint("viewpoints/addproperty/viewpoint.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    EObject A = getClassifier(l.get(0), "A").get();

    // Check the new property is created on A
    {
      EObject f = getFeature(A, "newProperty").get();
      assertEquals("newProperty", eGet(f, "name"));
      assertEquals(v.getVirtual(EcorePackage.Literals.ESTRING), eGet(f, "eType"));
      // It's not optional by default
      assertEquals(1, eGet(f, "lowerBound"));
      assertEquals(1, eGet(f, "upperBound"));
    }

    // Check the optional property is also created
    {
      EObject f = getFeature(A, "newOptionalProperty").get();
      assertEquals("newOptionalProperty", eGet(f, "name"));
      assertEquals(v.getVirtual(EcorePackage.Literals.EINT), eGet(f, "eType"));
      assertEquals(0, eGet(f, "lowerBound"));
      assertEquals(1, eGet(f, "upperBound"));
    }

    // The original metamodel is *not* modified
    assertEquals(1,
                 getFeatures(getClassifier(v.getContributingEPackages().get("A"), "A").get()).size());
  }

  @Test
  public void addCompositionAssociation() throws IOException {
    Viewpoint v = loadViewpoint("viewpoints/addassoc/composition.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    EObject A = getClassifier(l.get(0), "A").get();
    EObject B = getClassifier(l.get(1), "B").get();

    // Check the references exist with the right EType
    EObject AtoB = getFeature(A, "refToB").get();
    assertEquals(B, eGet(AtoB, "eType"));
    // And it's a containment
    assertEquals(true, eGet(AtoB, "containment"));

    // The original metamodel is *not* modified
    assertFalse(getFeature(getClassifier(v.getContributingEPackages().get("A"), "A").get(), "refToB").isPresent());
  }

  @Test
  public void addBidirectionalAssociation() throws IOException {
    // A new association from A to B should create an EReference in A with EType B.

    Viewpoint v = loadViewpoint("viewpoints/addassoc/bidirectional.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    EObject A = getClassifier(l.get(0), "A").get();
    EObject B = getClassifier(l.get(1), "B").get();

    // Check the references exist with the right EType
    EObject AtoB = getFeature(A, "refToB").get();
    assertEquals(B, eGet(AtoB, "eType"));
    EObject BtoA = getFeature(B, "refToA").get();
    assertEquals(A, eGet(BtoA, "eType"));

    // Check they are each other's opposite
    assertEquals(AtoB, eGet(BtoA, "eOpposite"));
    assertEquals(BtoA, eGet(AtoB, "eOpposite"));

    // The original metamodels are *not* modified
    A = getClassifier(v.getContributingEPackages().get("A"), "A").get();
    B = getClassifier(v.getContributingEPackages().get("B"), "B").get();
    assertFalse(getFeature(A, "refToB").isPresent());
    assertFalse(getFeature(B, "refToB").isPresent());
  }

  @Test
  public void addPropertyToNewConcept() throws IOException {
    // We can link virtual elements from NewConcept/NewProperties/NewAssociation.
    // E.g., we can add properties to a new concept in the same weaving model.

    Viewpoint v = loadViewpoint("viewpoints/synthetic-elements/property-to-newconcept.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(1);
    // It holds our new concept
    EObject C = getClassifier(p, "NewConcept").get();
    // And the new concept holds our new property
    assertTrue(getFeature(C, "newProperty").isPresent());
  }

  @Test
  public void addSuperconceptToNewConcept() throws IOException {
    // We can add concepts and a concept that generalizes those.

    Viewpoint v = loadViewpoint("viewpoints/synthetic-elements/concept-to-newconcept.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    EObject A = getClassifier(l.get(0), "A").get();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(1);
    // It holds our new concepts
    EObject C1 = getClassifier(p, "NewConcept").get();
    EObject C2 = getClassifier(p, "SuperConcept").get();
    // And the super concept has one existing and one new concept as subconcepts
    assertEquals(C2, eList(A, "eSuperTypes").get(0));
    assertEquals(C2, eList(C1, "eSuperTypes").get(0));

    // The original metamodel is *not* modified
    assertEquals(0, eList(getClassifier(v.getContributingEPackages().get("A"), "A").get(), "eSuperTypes").size());
  }

  @Test
  public void addAssociationToNewConcept() throws IOException {
    // We can add new properties and an association between them.

    Viewpoint v = loadViewpoint("viewpoints/synthetic-elements/assoc-to-newconcept.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    EObject A = getClassifier(l.get(0), "A").get();
    // The virtual package comes after packages from the contributing models
    EObject p = l.get(1);
    // It holds our new concept
    EObject C = getClassifier(p, "C").get();
    // There is a reference from A to C
    EObject AtoC = getFeature(A, "refToC").get();
    assertEquals(C, eGet(AtoC, "eType"));

    // The original model is *not* modified
    A = v.getContributingEPackages().get("A").getEClassifier("A");
    assertEquals(1, getFeatures(A).size());
  }

  @Test
  public void filterBlacklist() throws IOException {
    // Filtered elements should not exist on the viewpoint.

    Viewpoint v = loadViewpoint("viewpoints/filter/blacklist.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // There is only the A and B packages
    assertEquals(2, l.size());

    // A has no features, since it was filtered
    EObject A = getClassifier(l.get(0), "A").get();
    assertEquals(0, getFeatures(A).size());
    assertFalse(getFeature(A, "a").isPresent());

    // The original model is *not* modified
    A = v.getContributingEPackages().get("A").getEClassifier("A");
    assertEquals(1, getFeatures(A).size());

    // B has its feature, since it was not filtered
    EObject B = getClassifier(l.get(1), "B").get();
    assertTrue(getFeature(B, "b").isPresent());
  }

  @Test
  public void filterWhitelist() throws IOException {
    // In a weaving model in whitelist mode, filtered elements should be
    // the only remaining elements in the view.

    Viewpoint v = loadViewpoint("viewpoints/filter/whitelist.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // There is only the contentfwk package
    assertEquals(1, l.size());
    EObject p = l.get(0);

    // Package has only one classifier
    assertEquals(1, getClassifiers(p).size());
    EObject C = getClassifiers(p).get(0);

    // Only 1 feature is left
    assertEquals(1, getFeatures(C).size());
    assertTrue(getFeature(C, "ID").isPresent());

    // The original model is *not* modified
    p = v.getContributingEPackages().get("cfw");
    assertEquals(53, getClassifiers(p).size());
  }

  @Test
  public void filterWhitelistSyntheticElements() {
    // In a weaving model in whitelist mode, synthetic elements
    // are still present in the view

    // Metamodel and model
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')])",
        EcoreFactory.eINSTANCE)[0];

    // Weaving model
    WeavingModel wm = (WeavingModel) Sexp2EMF.build(
      "(WeavingModel :name 'WM' "
      + ":whitelist true "
      + ":virtualLinks [(VirtualAssociation :name 'assoc'"
      + "                :source @B"
      + "                :target @B"
      + "                :lowerBound 0 :upperBound -1)"
      + "               #B(VirtualConcept :name 'B')"
      + "               (VirtualProperty :name 'prop'"
      + "                :parent @B"
      + "                :type 'int')])",
      VirtualLinksFactory.eINSTANCE)[0];

    // Viewpoint
    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint viewpoint = new Viewpoint(m, wm);

    // The synthetic elements are present
    EPackage V = viewpoint.getRootPackage();
    EClass B = (EClass) findClassifier(V, "B");
    assertNotNull(B);
    assertNotNull(B.getEStructuralFeature("assoc"));
    assertNotNull(B.getEStructuralFeature("prop"));
  }

  @Test
  public void filterObject() {
    // We can filter objects from models

    // Metamodel and model
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A'"
        + "                :eStructuralFeatures [(EReference :name 'refToB'"
        + "                                       :eType @1)])"
        + "               #1(EClass :name 'B')])",
        EcoreFactory.eINSTANCE)[0];

    EObject[] model = Sexp2EMF.build("[#1(B) (A :refToB @1)]", P.getEFactoryInstance());

    // Sanity checks
    assertEquals("B", model[0].eClass().getName());
    assertEquals("A", model[1].eClass().getName());
    assertEquals(model[0], eGet(model[1], "refToB"));

    // Viewpoint
    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint viewpoint = new Viewpoint(m);

    // Add the model to a resource, so we can use its fragment URI to filter it
    Resource r = new ResourceImpl();
    r.getContents().addAll(Arrays.asList(model));

    String Buri = r.getURIFragment(model[0]);

    WeavingModel wm = (WeavingModel) Sexp2EMF.build(String.format(
      "(WeavingModel :name 'WM' "
      + ":contributingModels [(ContributingModel :URI '00'"
      + "                      :concreteElements [#1(ConcreteElement :path '%s')])]"
      + ":virtualLinks [(Filter :name 'P.B' :target @1)])",
      Buri), VirtualLinksFactory.eINSTANCE)[0];

    // View that filters B
    View view = new View(viewpoint, Arrays.asList(r), wm);

    // B is now filtered
    assertEquals(1, view.getVirtualContents().size());
    EObject A = view.getVirtualContents().get(0);
    assertNull(eGet(A, "refToB"));
  }

  @Test
  public void relativePaths() throws IOException {
    // An eviewpoint file should accept relative paths

    loadViewpoint("viewpoints/paths/relative.eviewpoint");
    // If no exception was raised, then it's fine
  }

  @Test
  public void absoluteFilePaths() throws IOException {
    // An eviewpoint file should accept absolute paths with the `file:` scheme

    Properties p = new Properties();
    p.setProperty(ViewpointResource.EVIEWPOINT_CONTRIBUTING_METAMODELS,
                  "min::" + (URI.createFileURI(here + "/resources/metamodels/minimalref.ecore").toString()));
    p.setProperty(ViewpointResource.EVIEWPOINT_WEAVING_MODEL,
                  URI.createFileURI(here + "/resources/viewpoints/minimal/weaving.xmi").toString());
    p.store(URIConverter.INSTANCE.createOutputStream(resourceURI("viewpoints/paths/absolute-file-scheme.eviewpoint")), null);

    loadViewpoint("viewpoints/paths/absolute-file-scheme.eviewpoint");

    // Delete the temporary eviewpoint file
    new File(here + "/resources/viewpoints/paths/absolute-file-scheme.eviewpoint").delete();
  }

  @Test
  public void absolutePathsWithoutScheme() throws IOException {
    // An eviewpoint file should accept absolute paths without any URI scheme

    Properties p = new Properties();
    p.setProperty(ViewpointResource.EVIEWPOINT_CONTRIBUTING_METAMODELS,
                  "min::" + (URI.createFileURI(here + "/resources/metamodels/minimalref.ecore").path()));
    p.setProperty(ViewpointResource.EVIEWPOINT_WEAVING_MODEL,
                  URI.createFileURI(here + "/resources/viewpoints/minimal/weaving.xmi").path());
    p.store(URIConverter.INSTANCE.createOutputStream(resourceURI("viewpoints/paths/absolute-no-scheme.eviewpoint")), null);

    loadViewpoint("viewpoints/paths/absolute-no-scheme.eviewpoint");

    // Delete the temporary eviewpoint file
    new File(here + "/resources/viewpoints/paths/absolute-no-scheme.eviewpoint").delete();
  }

  @Test
  public void absolutePlatformPaths() throws IOException {
    // An eviewpoint file should accept absolute paths with the `platform:` scheme

    String plugin = "org.atlanmod.emfviews.tests";

    Properties p = new Properties();
    p.setProperty(ViewpointResource.EVIEWPOINT_CONTRIBUTING_METAMODELS,
                  "min::" + (URI.createPlatformPluginURI(plugin + "/resources/metamodels/minimalref.ecore", true).toString()));
    p.setProperty(ViewpointResource.EVIEWPOINT_WEAVING_MODEL,
                  URI.createPlatformPluginURI(plugin + "/resources/viewpoints/minimal/weaving.xmi", true).toString());
    p.store(URIConverter.INSTANCE.createOutputStream(resourceURI("viewpoints/paths/absolute-platform-scheme.eviewpoint")), null);

    loadViewpoint("viewpoints/paths/absolute-platform-scheme.eviewpoint");

    // Delete the temporary eviewpoint file
    new File(here + "/resources/viewpoints/paths/absolute-platform-scheme.eviewpoint").delete();
  }

  @Test
  public void wildcardPath() throws IOException {
    // A '*' in a concrete element should include (or exclude) all
    // attributes.

    Viewpoint v = loadViewpoint("viewpoints/wildcard/wildcard.eviewpoint");

    EList<EObject> l = v.getRootPackage().eContents();
    // There is only the VirtualLinks package
    assertEquals(1, l.size());
    EObject p = l.get(0);

    // Package has only one classifier
    assertEquals(1, getClassifiers(p).size());
    EObject C = getClassifiers(p).get(0);

    // All features are present
    assertEquals(2, getFeatures(C).size());
    assertTrue(getFeature(C, "model").isPresent());
    assertTrue(getFeature(C, "path").isPresent());
  }

  @Test
  public void pureMemoryViewpoint() {
    // We should be able to create a viewpoint without creating any file

    // Construct the contributing package and weaving model
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')"
        + "               (EClass :name 'B')])",
        EcoreFactory.eINSTANCE)[0];

    WeavingModel WM = (WeavingModel) Sexp2EMF.build("(WeavingModel :name 'WM' "
        + ":contributingModels [(ContributingModel :URI '00'"
        + "                      :concreteElements [#1(ConcreteElement :path 'A')])]"
        + ":virtualLinks [(Filter :name 'P.A' :target @1)])",
        VirtualLinksFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint v = new Viewpoint(m, WM);
    EPackage VP = v.getRootPackage().getESubpackages().get(0);

    assertEquals(P.getName(), VP.getName());
    assertEquals(P.getNsURI(), VP.getNsURI());
    assertEquals(null, VP.getEClassifier("A"));
    assertEquals(v.getVirtual(P.getEClassifier("B")), VP.getEClassifier("B"));
  }

  @Test
  public void pureMemoryView() {
    // We should be able to create a view without creating any file

    // Construct the viewpoint
    EPackage P = (EPackage) Sexp2EMF.build("(EPackage :name 'P' :nsURI '00' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')"
        + "               (EClass :name 'B')])",
        EcoreFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P", P);
    Viewpoint viewpoint = new Viewpoint(m);

    // Construct the view
    EObject[] model = Sexp2EMF.build("[(A) (B)]", P.getEFactoryInstance());
    Resource r = new ResourceImpl();
    r.getContents().addAll(Arrays.asList(model));

    View view = new View(viewpoint, Arrays.asList(r));

    assertEquals(2, view.getVirtualContents().size());
    assertEquals(view.getVirtual(model[0]), view.getVirtualContents().get(0));
    assertEquals(view.getVirtual(model[1]), view.getVirtualContents().get(1));
  }

  // Helper to stay DRY
  static Object[] createViewpointOnViewpoint() {
    // The first package
    EPackage P0 = (EPackage) Sexp2EMF.build("(EPackage :name 'P0' :nsURI 'P0' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')])",
        EcoreFactory.eINSTANCE)[0];

    // The first viewpoint
    WeavingModel WM1 = (WeavingModel) Sexp2EMF.build("(WeavingModel :name 'WM1' "
        + ":contributingModels [(ContributingModel :URI 'P0'"
        + "                      :concreteElements [#1(ConcreteConcept :path 'A')])]"
        + ":virtualLinks [(VirtualConcept :name 'B' :superConcepts [@1])])",
        VirtualLinksFactory.eINSTANCE)[0];
    Map<String, EPackage> m = new HashMap<>();
    m.put("P0", P0);
    Viewpoint v1 = new Viewpoint(m, WM1);

    // The second viewpoint
    WeavingModel WM2 = (WeavingModel) Sexp2EMF.build("(WeavingModel :name 'WM2' "
        + ":contributingModels [(ContributingModel :URI 'http://www.atlanmod.org/emfviews/viewpoint/WM1/virtual'"
        + "                      :concreteElements [#1(ConcreteConcept :path 'B')])]"
        + ":virtualLinks [(VirtualConcept :name 'C' :superConcepts [@1])])",
        VirtualLinksFactory.eINSTANCE)[0];
    Map<String, EPackage> m1 = new HashMap<>();
    for (int i=0; i < v1.getRootPackage().getESubpackages().size(); ++i) {
      m1.put("P"+i, v1.getRootPackage().getESubpackages().get(i));
    }
    Viewpoint v2 = new Viewpoint(m1, WM2);

    return new Object[] {P0, v1, v2};
  }

  @Test
  public void viewpointOnViewpoint() {
    // We can create viewpoints on viewpoints

    Object[] v = createViewpointOnViewpoint();
    EPackage P2 = ((Viewpoint) v[2]).getRootPackage();
    EClass A = (EClass) findClassifier(P2, "A");
    EClass B = (EClass) findClassifier(P2, "B");
    EClass C = (EClass) findClassifier(P2, "C");

    // The hierarchy is as follows:
    // P2
    // |- P0             (from v[1])
    // |  `- A
    // |- virtualPackage (from v[1])
    // |  `- B
    // `- virtualPackage
    //    `- C

    assertEquals(3, P2.getESubpackages().size());
    assertEquals("P0", P2.getESubpackages().get(0).getName());
    assertEquals(A, P2.getESubpackages().get(0).getEClassifier("A"));
    assertEquals(B, P2.getESubpackages().get(1).getEClassifier("B"));
    assertEquals(C, P2.getESubpackages().get(2).getEClassifier("C"));

    // We should have A >: B >: C
    assertEquals(A, B.getESuperTypes().get(0));
    assertEquals(B, C.getESuperTypes().get(0));
  }

  @Test
  public void viewOnView() {
    // We can create views on views

    Object[] viewpoints = createViewpointOnViewpoint();
    EPackage P0 = (EPackage) viewpoints[0];

    // Now construct the views
    EObject[] m0 = Sexp2EMF.build("[(A) (A)]", P0.getEFactoryInstance());
    Resource r0 = new ResourceImpl();
    r0.getContents().addAll(Arrays.asList(m0));
    View v1 = new View((Viewpoint) viewpoints[1], Arrays.asList(r0));

    // There is no factory in views (yet), so we just wrap the first view
    ViewResource r1 = new ViewResource();
    r1.setView(v1);
    View v2 = new View((Viewpoint) viewpoints[2], Arrays.asList(r1));

    EList<EObject> contents = v2.getVirtualContents();

    assertEquals(2, contents.size());
    assertEquals("A", contents.get(0).eClass().getName());
    assertEquals("A", contents.get(1).eClass().getName());
  }

  @Test
  public void virtualClassHasPackage() {
    // When creating a viewpoint, a virtualClass.ePackage feature
    // points to the virtualization of its owning package (and vice-versa).

    EPackage P0 = (EPackage) Sexp2EMF.build("(EPackage :name 'P0' :nsURI 'P0' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')])",
        EcoreFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P0", P0);
    Viewpoint v = new Viewpoint(m);
    EPackage P1 = v.getRootPackage().getESubpackages().get(0);
    EClassifier A = P1.getEClassifier("A");

    assertNotNull(A);
    assertEquals(P1, A.getEPackage());
    assertEquals(P1, eGet(A, "ePackage"));
  }

  @Test
  public void pureVirtualClassHasPackage() {
    // When creating a viewpoint, a pure virtualClass.ePackage feature
    // points to the virtualization of its owning package (and vice-versa).

    EPackage P0 = (EPackage) Sexp2EMF.build("(EPackage :name 'P0' :nsURI 'P0' :nsPrefix 'P0' "
        + ":eClassifiers [(EClass :name 'A')])",
        EcoreFactory.eINSTANCE)[0];

    WeavingModel WM = (WeavingModel) Sexp2EMF.build("(WeavingModel :name 'WM1' "
        + ":virtualLinks [(VirtualConcept :name 'B')])",
        VirtualLinksFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P0", P0);
    Viewpoint v = new Viewpoint(m, WM);
    EPackage P1 = v.getRootPackage().getESubpackages().get(1);
    EClassifier B = P1.getEClassifier("B");

    assertNotNull(B);
    assertEquals(P1, B.getEPackage());
    assertEquals(P1, eGet(B, "ePackage"));
  }

  @Test
  public void filterInheritance() {
    // In an inheritance chain A <- B <- C, filtering B should still
    // retain the fact that A <- C indirectly.

    EPackage P0 = (EPackage) Sexp2EMF.build(
      "(EPackage :name 'P0' :nsURI 'P0' :nsPrefix 'P0' "
    + ":eClassifiers [#A(EClass :name 'A')"
    + "               #B(EClass :name 'B' :eSuperTypes [@A])"
    + "               #C(EClass :name 'C' :eSuperTypes [@B])])",
    EcoreFactory.eINSTANCE)[0];

    WeavingModel WM = (WeavingModel) Sexp2EMF.build(
      "(WeavingModel :name 'WM1' "
    + ":contributingModels [(ContributingModel :URI 'P0'"
    + "                     :concreteElements [#B(ConcreteConcept :path 'B')])] "
    + ":virtualLinks [(Filter :name 'B' :target @B)])",
    VirtualLinksFactory.eINSTANCE)[0];

    Map<String, EPackage> m = new HashMap<>();
    m.put("P0", P0);
    Viewpoint v = new Viewpoint(m, WM);
    EPackage P1 = v.getRootPackage().getESubpackages().get(0);
    EClass A = (EClass) P1.getEClassifier("A");
    EClass C = (EClass) P1.getEClassifier("C");

    assertEquals(Arrays.asList(A), C.getEAllSuperTypes());
  }

  @Test
  public void lazyECLRuleMatching() throws Exception {
    // A virtual feature + an ECL matching file will lazily call the ECL rule
    // only when accessing the feature for the first time

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/single-ref.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("assoc"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // B is present
      assertEquals(B, eGet(A, "assoc"));

      // Multiple accesses do not populate the features more than once
      assertEquals(B, eGet(A, "assoc"));
      assertEquals(B, eGet(A, "assoc"));

      // The rule has been executed exactly once
      assertEquals(1, (int)delegate.executeCounter.get("assoc"));
    }
  }

  @Test
  public void lazyECLRuleMatchingWithOpposite() throws Exception {
    // Accessing the opposite of a lazy rule still triggers the rule

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/opposite.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("refToB"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // A is in the opposite
      assertEquals(A, eGet(B, "refToA"));

      // The forward feature is populated
      assertEquals(B, eGet(A, "refToB"));

      // Multiple accesses do not populate the features more than once
      assertEquals(B, eGet(A, "refToB"));
      assertEquals(A, eGet(B, "refToA"));
      assertEquals(B, eGet(A, "refToB"));
      assertEquals(A, eGet(B, "refToA"));

      // The rule has been executed exactly twice
      assertEquals(2, (int)delegate.executeCounter.get("refToB"));
    }
  }

  @Test
  public void lazyECLRuleMatchingManyAssoc() throws Exception {
    // Idem but for virtual reference with -1 upper bound

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/many-ref.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("manyB"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // B is present
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // Multiple accesses do not populate the features more than once
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // The rule has been executed exactly once
      assertEquals(1, (int)delegate.executeCounter.get("manyB"));
    }
  }

  @Test
  public void lazyECLRuleMatchingManyAssocOpposite() throws Exception {
    // Idem but for virtual reference with -1 upper bound
    // browsing from the opposite feature first

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/many-ref.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("manyB"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // A is in the opposite
      assertEquals(Arrays.asList(A), eGet(B, "manyA"));

      // The forward feature is now populated
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // Multiple accesses do not populate the features more than once
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(Arrays.asList(A), eGet(B, "manyA"));
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(Arrays.asList(A), eGet(B, "manyA"));

      // The rule has been executed exactly twice
      assertEquals(2, (int)delegate.executeCounter.get("manyB"));
    }
  }

  @Test
  public void lazyECLRuleMatchingManyAssocAsymmetric() throws Exception {
    // Idem but for virtual reference with -1 upper bound
    // with an upper bound 1 opposite

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/many-ref-asymmetric.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("manyB"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // B is present
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // A is the opposite
      assertEquals(A, eGet(B, "refToA"));

      // The forward feature is now populated
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // Multiple accesses do not populate the features more than once
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(A, eGet(B, "refToA"));
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(A, eGet(B, "refToA"));

      // The rule has been executed exactly twice
      assertEquals(2, (int)delegate.executeCounter.get("manyB"));
    }
  }

  @Test
  public void lazyECLRuleMatchingManyAssocAsymmetricOpposite() throws Exception {
    // Idem but for virtual reference with -1 upper bound
    // with an upper bound 1 opposite, starting from the opposite

    // Setup
    try (CountExecutedRulesEclDelegate delegate = new CountExecutedRulesEclDelegate()) {
      View v = loadView("views/lazy-rule/many-ref-asymmetric.eview");

      // The rule has not been executed
      assertNull(delegate.executeCounter.get("manyB"));

      EList<EObject> c = v.getVirtualContents();
      EObject A = c.get(0);
      EObject B = c.get(1);

      // A is the opposite
      assertEquals(A, eGet(B, "refToA"));

      // The forward feature is now populated
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));

      // Multiple accesses do not populate the features more than once
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(A, eGet(B, "refToA"));
      assertEquals(Arrays.asList(B), eGet(A, "manyB"));
      assertEquals(A, eGet(B, "refToA"));

      // The rule has been executed exactly twice
      assertEquals(2, (int)delegate.executeCounter.get("manyB"));
    }
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Helpers for dealing with EMF resources and packages

  Viewpoint loadViewpoint(String path) throws IOException {
    ViewpointResource vr = new ViewpointResource(resourceURI(path));
    vr.load(null);
    assertNoErrors(vr);
    return vr.getViewpoint();
  }

  View loadView(String path) throws IOException {
    ViewResource vr = new ViewResource(resourceURI(path));
    vr.load(null);
    assertNoErrors(vr);
    return vr.getView();
  }

  /** Fail if the resource has errors */
  void assertNoErrors(Resource r) {
    if (r.getErrors().size() > 0) {
      StringJoiner sj = new StringJoiner("\n");
      r.getErrors().forEach(err -> sj.add(err.getMessage()));
      fail(sj.toString());
    }
  }

  /** Find classifier in `p` or its subpackage */
  EClassifier findClassifier(EPackage p, String classifierName) {
    EClassifier c = p.getEClassifier(classifierName);
    if (c != null) {
      return c;
    }

    for (EPackage sub : p.getESubpackages()) {
      c = findClassifier(sub, classifierName);
      if (c != null) {
        return c;
      }
    }

    return null;
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Helpers for reducing the boilerplate of calling the reflective EMF API

  Object eGet(EObject o, String featureName) {
    EStructuralFeature f = o.eClass().getEStructuralFeature(featureName);
    // Throw immediately rather than waiting for other objects to complain
    // about the missing feature
    if (f == null) {
      throw new NullPointerException();
    }
    return o.eGet(f);
  }

  void eSet(EObject o, String featureName, Object value) {
    EStructuralFeature f = o.eClass().getEStructuralFeature(featureName);
    if (f == null) {
      throw new NullPointerException();
    }
    o.eSet(f, value);
  }

  Optional<EObject> getClassifier(EObject o, String classifierName) {
    for (EObject c : getClassifiers(o)) {
      if (classifierName.equals(eGet(c, "name"))) {
        return Optional.of(c);
      }
    }
    return Optional.empty();
  }

  Optional<EObject> getFeature(EObject o, String featureName) {
    for (EObject c : getFeatures(o)) {
      if (featureName.equals(eGet(c, "name"))) {
        return Optional.of(c);
      }
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  EList<EObject> eList(EObject o, String featureName) {
    return (EList<EObject>) eGet(o, featureName);
  }

  EList<EObject> getClassifiers(EObject o) {
    return eList(o, "eClassifiers");
  }

  EList<EObject> getFeatures(EObject o) {
    return eList(o, "eStructuralFeatures");
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Other helpers

  // @Correctness: not sure if this is platform-agnostic
  static String here = new File(".").getAbsolutePath();

  URI resourceURI(String relativePath) {
    return URI.createFileURI(here + "/resources/" + relativePath);
  }

  // Helper class to replace the EclDelegate with one that counts the number of times
  // a given rule has been called.  Useful for testing lazy rules.
  // Implements AutoCloseable to restore the VirtualLinksDelegator state.
  static class CountExecutedRulesEclDelegate extends EclDelegate implements AutoCloseable {
    Map<String, Integer> executeCounter = new HashMap<>();

    public CountExecutedRulesEclDelegate() {
      VirtualLinksDelegator.register("ecl", this);
      VirtualLinksDelegator.skipRegistry = true;
    }

    @Override
    public List<EObject> executeMatchRule(String ruleName, EObject param, boolean rightHand) throws EolRuntimeException {
      Integer c = executeCounter.get(ruleName);
      executeCounter.put(ruleName, c == null ? 1 : c + 1);
      return super.executeMatchRule(ruleName, param, rightHand);
    }

    @Override
    public void close() throws Exception {
      VirtualLinksDelegator.skipRegistry = false;
    }
  }

}
