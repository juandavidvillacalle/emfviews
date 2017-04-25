/**
 * generated by Xtext
 */
package emfviews.dsl.generator;

import armines.execution.RunTransfoJava;

import com.google.common.collect.Iterables;

import emfviews.dsl.sqlview.Metamodel;
import emfviews.dsl.sqlview.Model;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;

/**
 * Generates code from your model files on save.
 * 
 * see http://www.eclipse.org/Xtext/documentation.html#TutorialCodeGeneration
 */
@SuppressWarnings("all")
public class VpdlGenerator implements IGenerator {
  public void doGenerate(final Resource resource, final IFileSystemAccess fsa) {
    CharSequence _compileEviewtype = this.compileEviewtype(resource);
    fsa.generateFile("myEAviewpoint.eviewtype", _compileEviewtype);
    CharSequence _compileEcl = this.compileEcl(resource);
    fsa.generateFile("myEAviewpoint.ecl", _compileEcl);
    CharSequence _compileXmi = this.compileXmi(resource);
    fsa.generateFile("myEAviewpoint.xmi", _compileXmi);
    //Generate .xmi
    RunTransfoJava rtj = new RunTransfoJava();
   //change the extension of vpdl to .xmi
    String inputModel = resource.getURI().toString().split("\\.")[0].concat(".xmi");
    System.out.println("inputModel: "+ inputModel);
    String filename = resource.getURI().toString().split("\\/")[resource.getURI().toString().split("\\/").length-1].toString();
    String outputModel = resource.getURI().toString().substring(0, resource.getURI().toString().indexOf(filename)).concat("myEAviewpoint.xmi");
    System.out.println("outputModel: "+ outputModel);
	rtj.runTransformation("http://www.dsl.emfviews/Sqlview", "http://www.dsl.emfviews/virtualLinks", "/transformations/SQL2VirtualLinks.asm",
			inputModel, outputModel);

    CharSequence _compileEcore = this.compileEcore(resource);
    fsa.generateFile("myEAviewpoint.ecore", _compileEcore);
  }
  
  public Iterable<Metamodel> getListMetamodels(final Resource r) {
    TreeIterator<EObject> _allContents = r.getAllContents();
    List<EObject> _list = IteratorExtensions.<EObject>toList(_allContents);
    return Iterables.<Metamodel>filter(_list, Metamodel.class);
  }
  
  public CharSequence compileEviewtype(final Resource r) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("contributingMetamodels=");
    _builder.newLine();
    {
      Iterable<Metamodel> _listMetamodels = this.getListMetamodels(r);
      for(final Metamodel e : _listMetamodels) {
        {
          EList<String> _metamodelURL = e.getMetamodelURL();
          String _string = _metamodelURL.toString();
          TreeIterator<EObject> _allContents = r.getAllContents();
          List<EObject> _list = IteratorExtensions.<EObject>toList(_allContents);
          Iterable<Metamodel> _filter = Iterables.<Metamodel>filter(_list, Metamodel.class);
          final Function1<Metamodel, Boolean> _function = new Function1<Metamodel, Boolean>() {
            public Boolean apply(final Metamodel it) {
              return Boolean.valueOf(true);
            }
          };
          Metamodel _findFirst = IterableExtensions.<Metamodel>findFirst(_filter, _function);
          EList<String> _metamodelURL_1 = _findFirst.getMetamodelURL();
          String _string_1 = _metamodelURL_1.toString();
          int _compareTo = _string.compareTo(_string_1);
          boolean _equals = (_compareTo == 0);
          if (_equals) {
            EList<String> _metamodelURL_2 = e.getMetamodelURL();
            String _string_2 = _metamodelURL_2.toString();
            EList<String> _metamodelURL_3 = e.getMetamodelURL();
            String _string_3 = _metamodelURL_3.toString();
            int _length = _string_3.length();
            int _minus = (_length - 2);
            String _substring = _string_2.substring(2, _minus);
            _builder.append(_substring, "");
            _builder.newLineIfNotEmpty();
          } else {
            EList<String> _metamodelURL_4 = e.getMetamodelURL();
            String _string_4 = _metamodelURL_4.toString();
            EList<String> _metamodelURL_5 = e.getMetamodelURL();
            String _string_5 = _metamodelURL_5.toString();
            int _length_1 = _string_5.length();
            int _minus_1 = (_length_1 - 2);
            String _substring_1 = _string_4.substring(2, _minus_1);
            _builder.append(_substring_1, "");
            _builder.append(",");
            _builder.newLineIfNotEmpty();
          }
        }
      }
    }
    _builder.newLine();
    _builder.append("correspondenceModel=EAview_Test/1_viewtype/");
    TreeIterator<EObject> _allContents_1 = r.getAllContents();
    List<EObject> _list_1 = IteratorExtensions.<EObject>toList(_allContents_1);
    Iterable<Model> _filter_1 = Iterables.<Model>filter(_list_1, Model.class);
    final Function1<Model, Boolean> _function_1 = new Function1<Model, Boolean>() {
      public Boolean apply(final Model it) {
        return Boolean.valueOf(true);
      }
    };
    Model _findFirst_1 = IterableExtensions.<Model>findFirst(_filter_1, _function_1);
    String _viewName = _findFirst_1.getViewName();
    _builder.append(_viewName, "");
    _builder.append(".xmi");
    _builder.newLineIfNotEmpty();
    _builder.append("correspondenceModelBase=EAview_Test/1_viewtype/");
    TreeIterator<EObject> _allContents_2 = r.getAllContents();
    List<EObject> _list_2 = IteratorExtensions.<EObject>toList(_allContents_2);
    Iterable<Model> _filter_2 = Iterables.<Model>filter(_list_2, Model.class);
    final Function1<Model, Boolean> _function_2 = new Function1<Model, Boolean>() {
      public Boolean apply(final Model it) {
        return Boolean.valueOf(true);
      }
    };
    Model _findFirst_2 = IterableExtensions.<Model>findFirst(_filter_2, _function_2);
    String _viewName_1 = _findFirst_2.getViewName();
    _builder.append(_viewName_1, "");
    _builder.append(".ecl");
    _builder.newLineIfNotEmpty();
    _builder.append("filtersMetamodel=/EAview_Test/1_viewtype/");
    TreeIterator<EObject> _allContents_3 = r.getAllContents();
    List<EObject> _list_3 = IteratorExtensions.<EObject>toList(_allContents_3);
    Iterable<Model> _filter_3 = Iterables.<Model>filter(_list_3, Model.class);
    final Function1<Model, Boolean> _function_3 = new Function1<Model, Boolean>() {
      public Boolean apply(final Model it) {
        return Boolean.valueOf(true);
      }
    };
    Model _findFirst_3 = IterableExtensions.<Model>findFirst(_filter_3, _function_3);
    String _viewName_2 = _findFirst_3.getViewName();
    _builder.append(_viewName_2, "");
    _builder.append(".ecore");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence compileEcl(final Resource r) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("//alias_togaf=http://www.obeonetwork.org/dsl/togaf/contentfwk/9.0.0");
    _builder.newLine();
    _builder.append("//alias_bpmn=http://www.omg.org/spec/BPMN/20100524/MODEL-XMI");
    _builder.newLine();
    _builder.append("//alias_reqif=http://www.omg.org/spec/ReqIF/20110401/reqif.xsd");
    _builder.newLine();
    _builder.newLine();
    _builder.append("rule detailedProcess");
    _builder.newLine();
    _builder.append("match s : togaf!Process");
    _builder.newLine();
    _builder.append("with  t : bpmn!Process");
    _builder.newLine();
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("compare");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return s.name = t.name;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("rule detailedRequirement");
    _builder.newLine();
    _builder.append("match s : togaf!Requirement");
    _builder.newLine();
    _builder.append("with  t : reqif!SpecObject");
    _builder.newLine();
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("compare");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("{");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return t.values.exists(v | v.theValue=s.name);");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence compileXmi(final Resource r) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence compileEcore(final Resource r) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    return _builder;
  }
}