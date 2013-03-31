package com.ibm

package plain

package hybriddb

package schema

package column

import java.io.{ File, FileOutputStream, ObjectInputStream, ObjectOutputStream, Closeable }
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.{ Paths, StandardOpenOption }

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import io.{ ByteBufferInputStream, LZ4 }

/**
 *
 */
final class MemoryMappedColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  val length: IndexType,

  pagefactor: Int,

  maxcachesize: Int,

  offsets: Array[Long],

  filepath: String)

  extends Column[A]

  with Closeable {

  println("pages " + offsets.length + " average pagesize " + (offsets.last / offsets.length))

  @inline final def get(index: IndexType): A = {
    page(index >> pagefactor)(index & mask)
  }

  override final def close = { file.close; cache.clear }

  private[this] final def page(i: Int) = cache.get(i) match {
    case Some(page) ⇒ page
    case _ ⇒
      val buf = file.map(READ_ONLY, offsets(i), offsets(i + 1) - offsets(i))
      val in = new ObjectInputStream(LZ4.newInputStream(new ByteBufferInputStream(buf)))
      val page = in.readObject.asInstanceOf[Array[A]]
      cache.add(i, page)
      page
  }

  private[this] final val file = FileChannel.open(Paths.get(filepath), StandardOpenOption.READ)

  private[this] final val cache = new collection.mutable.LruCache[Array[A]](maxcachesize, 0)

  private[this] final val mask = (1 << pagefactor) - 1

}

/**
 * pagefactor is n in 2 ^ n, for instance a pagefactor of 10 will result in 1024 entries per page
 */
final class MemoryMappedColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  capacity: IndexType,

  pagefactor: Int,

  maxcachesize: Int,

  filepath: String)

  extends ColumnBuilder[A, MemoryMappedColumn[A]] {

  /**
   * A good starting point, but should be fine tuned with the default constructor.
   */
  final def this(capacity: IndexType, filepath: String) = this(capacity, 10, 1024, filepath)

  final def next(value: A): Unit = {
    array.update(nextIndex & mask, value)
    if (0 == (length & mask)) {
      val os = new ObjectOutputStream(LZ4.newHighOutputStream(out))
      os.writeObject(array)
      os.flush
      offsets += file.length
    }
  }

  final def get = {
    if (0 != (length & mask)) {
      val os = new ObjectOutputStream(LZ4.newHighOutputStream(out))
      os.writeObject(array)
      os.flush
      offsets += file.length
    }
    out.close
    new MemoryMappedColumn[A](length, pagefactor, maxcachesize, offsets.toArray, filepath)
  }

  private[this] final val array = new Array[A](1 << pagefactor)

  private[this] final val offsets = { val p = new ArrayBuffer[Long]; p += 0L; p }

  private[this] final val file = new File(filepath)

  private[this] final val out = new FileOutputStream(file)

  private[this] final val mask = (1 << pagefactor) - 1

}
