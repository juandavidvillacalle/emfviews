package org.atlanmod.emfviews.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.InternalEList;

import org.atlanmod.emfviews.elements.VirtualEObject;

/**
 * A list of contents that delays the projection into virtual object of its
 * contents until they are accessed.
 *
 * This class is used in VirtualEObject to avoid virtualizing all contents up
 * front.
 */
public class LazyEContentsList implements EList<EObject>, InternalEList<EObject> {
  // @Correctness: only the essential EList methods are implemented.  This is
  // enough to make VirtualEObject work in most cases, but surely we should take
  // a look at the remaining methods.

  private VirtualEObject owner;

  // Cached values
  private EList<EReference> containments;
  private int numContainments = -1;
  private Object[] containedValues;
  private int[] containedSizes;
  private int size = -1;

  public LazyEContentsList(VirtualEObject owner) {
    this.owner = owner;
  }

  private EList<EReference> containments() {
    if (containments == null) {
      containments = owner.eClass().getEAllContainments();
      numContainments = containments.size();
    }
    return containments;
  }

  private int numContainments() {
    if (numContainments == -1) {
      numContainments = containments().size();
    }
    return numContainments;
  }

  private Object containedValue(int index) {
    if (containedValues == null) {
      containedValues = new Object[numContainments()];
    }

    if (containedValues[index] == null) {
      // What if eGet returns null?  We will call eGet again.
      // We could differentiate between null (unfetched) and null (fetched).
      containedValues[index] = owner.eGet(containments().get(index));
    }

    return containedValues[index];
  }

  private int containedSize(int index) {
    if (containedSizes == null) {
      containedSizes = new int[numContainments()];
      Arrays.fill(containedSizes, -1);
    }

    if (containedSizes[index] == -1) {
      if (containments().get(index).isMany()) {
        containedSizes[index] = ((List<?>) containedValue(index)).size();
      } else {
        containedSizes[index] = containedValue(index) == null ? 0 : 1;
      }
    }

    return containedSizes[index];
  }

  @Override
  public int size() {
    if (size == -1) {
      size = 0;
      for (int i=0; i < numContainments(); ++i) {
        size += containedSize(i);
      }
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    // If there is at least one element, then it's not empty.
    // This is faster than getting the actual size.
    for (int i=0; i < numContainments(); ++i) {
      if (containedSize(i) > 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<EObject> iterator() {
    return new Iterator<EObject>() {
      int cursor = 0;

      @Override
      public boolean hasNext() {
        return cursor < size();
      }

      @Override
      public EObject next() {
        return get(cursor++);
      }
    };
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(EObject e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends EObject> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends EObject> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public EObject get(int index) {
    for (int i=0; i < numContainments(); ++i) {
      int s = containedSize(i);
      if (index < s) {
        Object v = containedValue(i);
        if (containments().get(i).isMany()) {
          @SuppressWarnings("unchecked")
          List<EObject> list = (List<EObject>) v;
          return list.get(index);
        } else {
          return (EObject) v;
        }
      } else {
        index -= s;
      }
    }

    throw new IndexOutOfBoundsException(index + "");
  }

  @Override
  public EObject set(int index, EObject element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, EObject element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EObject remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<EObject> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<EObject> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<EObject> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EObject basicGet(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<EObject> basicList() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<EObject> basicIterator() {
    return iterator();
  }

  @Override
  public ListIterator<EObject> basicListIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<EObject> basicListIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] basicToArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] basicToArray(T[] array) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int basicIndexOf(Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int basicLastIndexOf(Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean basicContains(Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean basicContainsAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NotificationChain basicRemove(Object object, NotificationChain notifications) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NotificationChain basicAdd(EObject object, NotificationChain notifications) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addUnique(EObject object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addUnique(int index, EObject object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAllUnique(Collection<? extends EObject> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAllUnique(int index, Collection<? extends EObject> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EObject setUnique(int index, EObject object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void move(int newPosition, EObject object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EObject move(int newPosition, int oldPosition) {
    throw new UnsupportedOperationException();
  }

}
