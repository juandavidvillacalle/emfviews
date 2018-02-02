/*
 * generated by Xtext 2.12.0
 */
package fr.inria.atlanmod.emfviews.vpdl.ui.contentassist

import org.eclipse.xtext.Assignment
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.emf.ecore.EPackage
import fr.inria.atlanmod.emfviews.vpdl.vpdl.From

/*
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class VpdlProposalProvider extends AbstractVpdlProposalProvider {
  
  def void completeMetamodel_NsURI(From f, Assignment assignment, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
     super.completeMetamodel_NsURI(f, assignment, context, acceptor)
     
     for (key : EPackage.Registry.INSTANCE.keySet) {
       acceptor.accept(createCompletionProposal(key, context))
     }
  }
  
}