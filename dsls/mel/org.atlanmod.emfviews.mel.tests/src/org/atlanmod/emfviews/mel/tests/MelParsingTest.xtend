/*******************************************************************************
 * Copyright (c) 2018 Armines
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

package org.atlanmod.emfviews.mel.tests

import static org.hamcrest.CoreMatchers.*;

import com.google.inject.Inject
import org.atlanmod.emfviews.mel.mel.Model
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(MelInjectorProvider)
class MelParsingTest {

  @Inject ParseHelper<Model> parseHelper

  @Test
  def void empty() {
    val result = parseHelper.parse('')
    Assert.assertNull(result)
  }

  @Test
  def void minimal() {
    val result = parseHelper.parse('''
      import mm from 'uri'
      define extName extending mm {
        add class C
      }
    ''')
    Assert.assertNotNull(result)
    Assert.assertThat(result.eResource.errors, is(emptyList))
  }

  @Test
  def void allConstructs() {
    val result = parseHelper.parse('''
      import uml from 'http://www.eclipse.org/uml2/5.0.0/UML'
      import bpmn from 'http://www.omg.org/spec/BPMN/20100524/MODEL-XMI'

      define extension1 extending mm {
        add class X
        add class Y specializing uml.Class, uml.Abstraction
        add class Z supertyping uml.Activity, uml.Action, X
        add class B specializing Y supertyping Z

        modify class X {
          add property foo : EString 0
          add composition ref : X 1..1
          add association ref : X 1..1
          add constraint id 'constraint'
        }

        modify class uml.Class {
          add property opt : EString 0
          add property nonOpt : EString 1
          add composition refC : Y 0..*
          add association ref : uml.Action 1..*
          add constraint constraint1 "one e1"
        }

        modify class bpmn.Activity {
          modify association default {
            name defaultRenamed
            type X
            cardinality 0..*
            relation-type association
          }
          filter constraint constraint2
        }

        modify class uml.Behavior {
          modify property isReentrant {
            type EString
            cardinality 0
          }
          filter property isReentrant
        }

        modify class uml.Activity {
          modify property isReadOnly {
            type EBoolean
            cardinality 0
          }
        }
        // a comment
        filter class uml.DataType
      }
    ''')
    Assert.assertNotNull(result)
    Assert.assertThat(result.eResource.errors, is(emptyList))
  }

  @Test
  def void cannotModifyAttributeOfVirtualClass() {
    val result = parseHelper.parse('''
      import mm from ''
      define e extending mm {
        add class X
        modify class X {
          modify property a {}
        }
      }
    ''')
    Assert.assertNotNull(result)
    // That's a good place to test better error messages
    Assert.assertThat(result.eResource.errors, not(is(emptyList)))
  }

}
