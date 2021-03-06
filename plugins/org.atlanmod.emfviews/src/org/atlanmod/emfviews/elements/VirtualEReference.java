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

package org.atlanmod.emfviews.elements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreEList;

import org.atlanmod.emfviews.core.EcoreVirtualizer;

public class VirtualEReference extends VirtualEStructuralFeature<EReference> implements EReference {

  public EReference virtualOpposite;

  public VirtualEReference(EReference concreteReference, EcoreVirtualizer virtualizer) {
    super(EcorePackage.Literals.EREFERENCE, concreteReference, virtualizer);
  }

  @Override
  public Object dynamicGet(int dynamicFeatureID) {
    EStructuralFeature feature = eDynamicFeature(dynamicFeatureID);

    if (feature == EcorePackage.Literals.EREFERENCE__CONTAINER) {
      return isContainer();
    }
    if (feature == EcorePackage.Literals.EREFERENCE__CONTAINMENT) {
      return isContainment();
    }
    if (feature == EcorePackage.Literals.EREFERENCE__EKEYS) {
      return getEKeys();
    }
    if (feature == EcorePackage.Literals.EREFERENCE__EOPPOSITE) {
      return getEOpposite();
    }
    if (feature == EcorePackage.Literals.EREFERENCE__EREFERENCE_TYPE) {
      return getEReferenceType();
    }
    if (feature == EcorePackage.Literals.EREFERENCE__RESOLVE_PROXIES) {
      return isResolveProxies();
    }

    return super.dynamicGet(dynamicFeatureID);
  }

  @Override
  public boolean isResolveProxies() {
    return concrete().isResolveProxies();
  }

  @Override
  public boolean isContainer() {
    return concrete().isContainer();
  }

  @Override
  public void setContainment(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setResolveProxies(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEOpposite(EReference value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EReference getEOpposite() {
    EReference opposite;

    if (virtualOpposite != null) {
      opposite = virtualOpposite;
    } else {
      opposite = virtualizer.getVirtual(concrete().getEOpposite());
    }

    if (opposite != null) {
      if (((VirtualEReference) opposite).isVisible()) {
        return opposite;
      }
    }

    return null;
  }

  @Override
  public EClass getEReferenceType() {
    return virtualizer.getVirtual(concrete().getEReferenceType());
  }

  @Override
  public EList<EAttribute> getEKeys() {
    List<EAttribute> keys = new ArrayList<>();

    for (EAttribute a : concrete().getEKeys()) {
      VirtualEAttribute va = virtualizer.getVirtual(a);
      if (va.isVisible()) {
        keys.add(va);
      }
    }

    // See @UnmodifiableEList.
    return new EcoreEList.UnmodifiableEList<>(this,
        EcorePackage.Literals.EREFERENCE__EKEYS, keys.size(), keys.toArray());
  }

}
