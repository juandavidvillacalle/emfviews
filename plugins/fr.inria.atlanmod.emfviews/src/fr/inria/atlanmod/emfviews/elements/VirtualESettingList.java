package fr.inria.atlanmod.emfviews.elements;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class VirtualESettingList<E> extends VirtualEList<E> implements EStructuralFeature.Setting {

  private EObject owner;
  private EStructuralFeature feature;

  public VirtualESettingList(EList<E> concreteList, Virtualizer virtualizer, EObject owner, EStructuralFeature feature) {
    super(concreteList, virtualizer);
    this.owner = owner;
    this.feature = feature;
  }

  @Override
  public EObject getEObject() {
    return owner;
  }

  @Override
  public EStructuralFeature getEStructuralFeature() {
    return feature;
  }

  @Override
  public Object get(boolean resolve) {
    return this;
  }

  @Override
  public void set(Object newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSet() {
    return !isEmpty();
  }

  @Override
  public void unset() {
    throw new UnsupportedOperationException();
  }



}