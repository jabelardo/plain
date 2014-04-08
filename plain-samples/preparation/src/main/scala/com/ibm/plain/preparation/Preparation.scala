package com.ibm

package plain

package preparation

import scala.util.Random.nextInt
import scala.annotation.tailrec
import scala.language.implicitConversions

import concurrent.scheduleOnce
import bootstrap.ExternalComponent
import logging.Logger

/**
 *
 */
final class Preparation

  extends ExternalComponent[Preparation]("preparation")

  with Logger {

  import Preparation._

  override def start = {
    testing
    scheduleOnce(200)(application.teardown)
    this
  }

}

/**
 * Playground.
 */
object Preparation

  extends Logger {

  def dump[A](prefix: String, list: Iterable[A]) = println(prefix + list.mkString("\n", "\n", ""))

  object Source extends Enumeration { type Source = Value; val Enovia, Windchill = Value }; import Source._

  trait HasId {

    val id = HasId.next

  }

  object HasId {

    private def next = { c += 1; ("Uuid" + c).hashCode.toString }

    private var c = 0

  }

  trait Master

    extends HasId {

    val name: String

  }

  trait Version

    extends HasId {

    val version: Int

    val isNew: Boolean

    val isModified: Boolean

    val state = if (isNew) "new" else if (isModified) "modified" else "unmodified"

    require(!(isNew && isModified))

  }

  trait Reference extends Master with Version {

    val isAssembly: Boolean

    val source: Source

    def fromEnovia = source == Enovia

    def fromWindchill = source == Windchill

    def filename = (if (fromEnovia) name + "_" + version else name) + (if (isAssembly) ".CATProduct" else ".CATPart")

    def more: String

    override def toString = source + "(" + name + " " + version + " " + (if (isAssembly) "assembly" else "single") + ", " + state + " " + filename + more + ")"

  }

  trait Assembly extends Reference {

    val isAssembly = true

    val isRoot: Boolean

    var assemblyrelations: List[AssemblyRelation] = Nil

    def addRelation(assemblyrelation: AssemblyRelation) = assemblyrelations = assemblyrelation :: assemblyrelations

    def more = (if (isRoot) " root" else "")

  }

  trait SinglePart extends Reference {

    val isAssembly = false

    def more = ""

  }
  
  case class AssemblyRelation(

    parent: Assembly,

    child: Reference)

    extends HasId {

    import AssemblyRelation._

    def relativeposition: (Double, Double, Double) = (1, 0, 0)

    parent.addRelation(this)

    override def toString = "AssemblyRelation(id=" + id + " parent=" + parent.name + " child=" + child.name + ")"

  }

  trait FromEnovia extends Reference { val source = Enovia }

  trait FromWindchill extends Reference { val source = Windchill }

  case class EnoviaAssembly(

    name: String,

    version: Int,

    isRoot: Boolean,

    isNew: Boolean,

    isModified: Boolean)

    extends Assembly

    with FromEnovia

  case class EnoviaSingle(

    name: String,

    version: Int,

    isNew: Boolean,

    isModified: Boolean)

    extends SinglePart

    with FromEnovia

  case class ProductStructure(root: Assembly) {

    def relations = root.assemblyrelations.foldLeft(List[AssemblyRelation]())((res, a) ⇒ a :: (a.child match { case b: Assembly ⇒ b.assemblyrelations case _ ⇒ Nil }) ::: res)

    def references = (root :: relations.map(_.child)).toSet

    def documents = references.map(_.filename)

    require(root.isRoot)

  }

  def testing = {

    val a = EnoviaAssembly("A", 1, true, true, false)
    val b = EnoviaSingle("B", 1, true, false)
    val c = EnoviaAssembly("C", 1, false, true, false)
    val d = EnoviaSingle("D", 1, true, false)
    val e = EnoviaSingle("E", 1, true, false)
    val ab = AssemblyRelation(a, b)
    val ac = AssemblyRelation(a, c)
    val cd = AssemblyRelation(c, d)
    val ce1 = AssemblyRelation(c, e)
    val ce2 = AssemblyRelation(c, e)
    val ps = ProductStructure(a)

    dump("relations", ps.relations)
    dump("references", ps.references)
    dump("documents: ", ps.documents)
    
    Modules.test
    
  }

}