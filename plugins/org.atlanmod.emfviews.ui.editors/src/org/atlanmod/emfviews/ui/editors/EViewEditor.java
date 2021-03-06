package org.atlanmod.emfviews.ui.editors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class EViewEditor extends EditorPart {
  private TreeViewer treeViewer;

  @Override
  public void createPartControl(Composite parent) {
    treeViewer = new TreeViewer(parent, SWT.VIRTUAL);

    IFile file = ((IFileEditorInput) getEditorInput()).getFile();
    URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);

    Resource v = (new ResourceSetImpl()).createResource(uri);

    try {
      v.load(null);
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    treeViewer.setContentProvider(new ILazyTreeContentProvider() {

      @Override
      public Object getParent(Object element) {
        if (element instanceof EObject) {
          return ((EObject) element).eContainer();
        } else {
          return null;
        }
      }

      @Override
      public void updateElement(Object parent, int index) {
        if (parent instanceof Resource) {
          Resource r = (Resource) parent;
          EObject child = r.getContents().get(index);
          treeViewer.replace(parent, index, child);
          treeViewer.setHasChildren(child, !child.eContents().isEmpty());
        } else if (parent instanceof EObject) {
          EObject e = (EObject) parent;
          EObject child = e.eContents().get(index);
          treeViewer.replace(parent, index, child);
          treeViewer.setHasChildren(child, !child.eContents().isEmpty());
        }
      }

      @Override
      public void updateChildCount(Object element, int currentChildCount) {
        if (element instanceof Resource) {
          Resource r = (Resource) element;
          treeViewer.setChildCount(element, r.getContents().size());
        } else if (element instanceof EObject) {
          EObject e = (EObject) element;
          treeViewer.setChildCount(element, e.eContents().size());
        }
      }
    });
    treeViewer.setLabelProvider(new VirtualEObjectLabelProvider());

    treeViewer.setUseHashlookup(true);
    treeViewer.setInput(v);
    getEditorSite().setSelectionProvider(treeViewer);

    // Refresh on right-click
    // @Refactor: we should probably use a command for that
    treeViewer.getControl().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent event) {
        if (event.button == 3) {

          // Save the expanded state
          Object[] expandedElements = treeViewer.getExpandedElements();

          Resource r = (new ResourceSetImpl()).createResource(uri);
          try {
            r.load(null);
          } catch (IOException ex) {
            ex.printStackTrace();
          }

          treeViewer.setInput(new Object[] { r });

          // Restore the expanded state
          // Find the equivalent object in the new resource, and expand it
          for (Object o : expandedElements) {
            // We don't care about Resource (they are at the top-level)
            if (o instanceof EObject) {
              // @Correctness: the object path is not quite accurate for this purpose
              // some objects have no name, but still have children
              String fqn = getEObjectPath((EObject) o);
              // @Optimize: that's a dumb and slow way to do it
              TreeIterator<EObject> it = r.getAllContents();
              while (it.hasNext()) {
                EObject ro = it.next();
                if (fqn.equals(getEObjectPath(ro))) {
                  treeViewer.setExpandedState(ro, true);

                  // Expand only the first match
                  break;
                }
              }
            } else if (o instanceof Resource) {
              treeViewer.setExpandedState(r, true);
            }
          }
        }
      }
    });
  }

  protected static String getEObjectPath(EObject o) {
    List<String> comps = new ArrayList<>();

    while (o != null) {
      EStructuralFeature nameFeature = o.eClass().getEStructuralFeature("name");
      if (nameFeature != null) {
        comps.add((String) o.eGet(nameFeature));
      }
      o = o.eContainer();
    }

    Collections.reverse(comps);
    return String.join(".", comps);
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    return super.getAdapter(adapter);
  }

  @Override
  public void setFocus() {
    treeViewer.getControl().setFocus();
  }

  @Override
  public void doSave(IProgressMonitor monitor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doSaveAs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    if (!(input instanceof IFileEditorInput)) {
      throw new PartInitException("Invalid Input: Must be IFileEditorInput");
    }

    setSite(site);
    setInput(input);
    setPartName(input.getName());
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

}
