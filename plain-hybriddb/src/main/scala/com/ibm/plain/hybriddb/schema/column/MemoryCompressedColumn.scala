package com.ibm

package plain

package hybriddb

package schema

package column

import java.io.{ ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import collection.mutable.LeastRecentlyUsedCache
import io.{ ByteArrayInputStream, LZ4 }

/**
 *
 */
@SerialVersionUID(1L) final class MemoryCompressedColumn[@specialized A](

  val length: Long,

  pagefactor: Int,

  maxcachesize: Int,

  pages: Array[Array[Byte]])

  extends BuiltColumn[A] {

  type Builder = MemoryCompressedColumnBuilder[A]

  override final def toString = "MemoryCompressedColumn(len=" + length + " pagefactor=" + pagefactor + " pages=" + pages.length + " pagesize(avg)=" + (pages.foldLeft(0L) { case (l, e) ⇒ l + e.length } / pages.length) + ")"

  final def get(index: Long): A = page(index.toInt >> pagefactor)(index.toInt & mask)

  private[this] final def page(i: Int) = cache.get(i) match {
    case Some(page) ⇒ page
    case _ ⇒
      val in = new ObjectInputStream(LZ4.newInputStream(new ByteArrayInputStream(pages(i))))
      val page = in.readObject.asInstanceOf[Array[A]]
      cache.put(i, page)
      page
  }

  private[this] final val cache = LeastRecentlyUsedCache[Array[A]](maxcachesize)

  private[this] final val mask = (1 << pagefactor) - 1

}

/**
 * pagefactor is n in 2 ^ n == entries per page, for instance a pagefactor of 10 will result in 1024 entries per page
 */
final class MemoryCompressedColumnBuilder[@specialized A: ClassTag](

  val capacity: Long,

  pagefactor: Int,

  maxcachesize: Int,

  highcompression: Boolean)

  extends ColumnBuilder[A, MemoryCompressedColumn[A]] {

  /**
   * A good starting point, but should be fine tuned with the default constructor.
   */
  final def this(capacity: Long) = this(capacity, 10, 1024, false)

  final def next(value: A): Unit = {
    array.update(nextIndex.toInt & mask, value)
    if (0 == (length.toInt & mask)) {
      out.reset
      val os = new ObjectOutputStream(if (highcompression) LZ4.newHighOutputStream(out) else LZ4.newFastOutputStream(out))
      os.writeObject(array)
      os.flush
      arrays += out.toByteArray
    }
  }

  final def result = {
    if (0 < (length.toInt & mask)) {
      out.reset
      val os = new ObjectOutputStream(if (highcompression) LZ4.newHighOutputStream(out) else LZ4.newFastOutputStream(out))
      os.writeObject(array)
      os.flush
      arrays += out.toByteArray
    }
    new MemoryCompressedColumn[A](length, pagefactor, maxcachesize, arrays.toArray)
  }

  private[this] final val arrays = new ArrayBuffer[Array[Byte]]

  private[this] final val array = new Array[A](1 << pagefactor)

  private[this] final val out = new ByteArrayOutputStream(8 * 1024)

  private[this] final val mask = (1 << pagefactor) - 1

}

