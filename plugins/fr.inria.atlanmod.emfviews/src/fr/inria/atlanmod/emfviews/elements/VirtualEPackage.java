package fr.inria.atlanmod.emfviews.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.util.EcoreEList;

public class VirtualEPackage extends DynamicEObjectImpl implements EPackage {

  private EPackage concreteEPackage;
  private List<VirtualEClass> virtualClassifiers = new ArrayList<>();
  private Set<EClassifier> filteredClassifiers = new HashSet<>();
  private Virtualizer virtualizer;

  public VirtualEPackage(EPackage concreteEPackage, Virtualizer virtualizer) {
    super(EcorePackage.Literals.EPACKAGE);
    this.concreteEPackage = concreteEPackage;
    this.virtualizer = virtualizer;
  }

  // @Correctness: should we accept VirtualClassifier (a supertype) as well?
  public void addVirtualClassifier(VirtualEClass f) {
    virtualClassifiers.add(f);
  }

  public int getVirtualClassifiersSize() {
    return virtualClassifiers.size();
  }

  public void filterClassifier(EClassifier c) {
    if (getEClassifiers().contains(c)) {
      filteredClassifiers.add(c);
    } else {
      throw new IllegalArgumentException("Classifier already filtered");
    }
  }

  public void unfilterClassifier(EClassifier c) {
    filteredClassifiers.remove(c);
  }

  public boolean isClassifierFiltered(EClassifier c) {
    return filteredClassifiers.contains(c);
  }

  @Override
  public Object dynamicGet(int dynamicFeatureID) {
    EStructuralFeature feature = eDynamicFeature(dynamicFeatureID);

    if (feature == EcorePackage.Literals.EPACKAGE__ECLASSIFIERS) {
      return getEClassifiers();
    }
    if (feature == EcorePackage.Literals.EPACKAGE__ESUBPACKAGES) {
      return getESubpackages();
    }
    if (feature == EcorePackage.Literals.ENAMED_ELEMENT__NAME) {
      return getName();
    }
    if (feature == EcorePackage.Literals.EMODEL_ELEMENT__EANNOTATIONS) {
      return getEAnnotations();
    }

    // @Correctness: reflexive access for other methods of the metaclass
    throw new IllegalArgumentException("Unknown feature: " + feature.getName());
  }

  @Override
  public void dynamicSet(int dynamicFeatureID, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dynamicUnset(int dynamicFeatureID) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String getName() {
    return concreteEPackage.getName();
  }

  @Override
  public void setName(String value) {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public EList<EAnnotation> getEAnnotations() {
    return concreteEPackage.getEAnnotations();
  }

  @Override
  public EAnnotation getEAnnotation(String source) {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public String getNsURI() {
    return concreteEPackage.getNsURI();
  }

  @Override
  public void setNsURI(String value) {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public String getNsPrefix() {
    return concreteEPackage.getNsPrefix();
  }

  @Override
  public void setNsPrefix(String value) {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public EFactory getEFactoryInstance() {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEFactoryInstance(EFactory value) {
    // TODO: Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  // @Refactor: quite close to getAllFeatures in VirtualEClass
  protected List<EClassifier> getAllClassifiers() {
    // @Optimize: an iterator would be best here

    List<EClassifier> elems = new ArrayList<>();

    for (EClassifier f : concreteEPackage.getEClassifiers()) {
      elems.add(virtualizer.getVirtual(f));
    }

    for (VirtualEClass f : virtualClassifiers) {
      elems.add(f);
    }

    return elems;
  }

  // @Refactor: quite close to getAllFeatures in VirtualEClass
  protected List<EClassifier> getNonFilteredClassifiers() {
    // @Optimize: an iterator would be best here

    List<EClassifier> elems = new ArrayList<>();

    for (EClassifier f : getAllClassifiers()) {
      if (!isClassifierFiltered(f)) {
        elems.add(f);
      }
    }

    return elems;
  }


  @Override
  public EList<EClassifier> getEClassifiers() {
    // FIXME: the return value must be castable to EStructuralFeature.Setting
    List<EClassifier> cs = getNonFilteredClassifiers();
    return new EcoreEList.UnmodifiableEList<>(
        this, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, cs.size(), cs.toArray());

    //return ECollections.unmodifiableEList(getNonFilteredClassifiers());
  }

  @Override
  public EList<EPackage> getESubpackages() {
    return new VirtualESettingList<>(concreteEPackage.getESubpackages(), virtualizer,
        this, EcorePackage.Literals.EPACKAGE__ESUBPACKAGES);
  }

  @Override
  public EPackage getESuperPackage() {
    EPackage sup = concreteEPackage.getESuperPackage();
    if (sup != null) {
      return virtualizer.getVirtual(sup);
    } else {
      return sup;
    }
  }

  @Override
  public EClassifier getEClassifier(String name) {
    for (EClassifier f : getEClassifiers()) {
      if (name.equals(f.getName())) {
        return f;
      }
    }
    return null;
  }

}
