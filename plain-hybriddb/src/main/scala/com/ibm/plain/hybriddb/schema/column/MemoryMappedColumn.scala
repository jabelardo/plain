package com.ibm

package plain

package hybriddb

package schema

package column

import java.io.{ EOFException, File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, OutputStream, Closeable }
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.{ Paths, StandardOpenOption }
import java.util.concurrent.atomic.AtomicInteger

import scala.annotation.tailrec
import scala.collection.Seq
import scala.collection.mutable.{ ArrayBuffer, WrappedArray }
import scala.concurrent.{ Await, Future, TimeoutException }
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{ Type, TypeTag, typeOf }

import collection.mutable.LeastRecentlyUsedCache
import collection.immutable.Sorting.{ binarySearch, sortedIndexedSeq }
import concurrent.{ future, parallelism }
import io.{ ByteBufferInputStream, LZ4 }
import reflect.ignore

/**
 *
 */
final class MemoryMappedColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A] private[column] (

  val name: String,

  val length: IndexType,

  pagefactor: Int,

  maxcachesize: Int,

  pages: Array[Long],

  indexpages: Array[Long],

  filepath: String,

  withordering: Option[Ordering[A]])

  extends Column[A]

  with BaseIndexed[A]

  with Closeable {

  outer ⇒

  override final def toString = "MemoryMappedColumn(len=" + length + " pagefactor=" + pagefactor + " pages=" + (pages.length - 1) + " pagesize(avg)=" + (pages.last / pages.length) + " filesize=" + pages.last + " ordered=" + withordering.isDefined + ")"

  final def get(index: IndexType): A = page(index >> pagefactor)(index & mask)

  override final def close = { file.close; cache.clear; if (null != indexfile) indexfile.close }

  private[this] final def page(i: Int) = cache.get(i) match {
    case Some(page) ⇒ page
    case _ ⇒
      val buf = file.map(READ_ONLY, pages(i), pages(i + 1) - pages(i))
      val in = new ObjectInputStream(LZ4.newInputStream(new ByteBufferInputStream(buf)))
      val page = in.readObject.asInstanceOf[Array[A]]
      cache.put(i, page)
      page
  }

  private[this] final def indexpage(i: Int) = indexcache.get(i) match {
    case Some(page) ⇒ page
    case _ ⇒
      val buf = indexfile.map(READ_ONLY, indexpages(i), indexpages(i + 1) - indexpages(i))
      val in = new ObjectInputStream(new ByteBufferInputStream(buf))
      val page = in.readObject.asInstanceOf[Array[IndexType]]
      indexcache.put(i, page)
      page
  }

  protected final val ordering = withordering.getOrElse(null)

  protected final val values = new WrappedArray[A] {
    final def length = outer.length
    final def apply(i: Int) = get(i)
    final def update(i: Int, value: A) = throw null
    final def array = throw null
    final def elemTag = throw null
  }

  protected final val array = new WrappedArray[IndexType] {
    final def length = outer.length
    final def apply(i: Int) = indexpage(i >> pagefactor)(i & mask)
    final def update(i: Int, value: Int) = throw null
    final def array = throw null
    final def elemTag = throw null
  }

  private[this] final val file = FileChannel.open(Paths.get(filepath), StandardOpenOption.READ)

  private[this] final val indexfile = if (withordering.isDefined) FileChannel.open(Paths.get(filepath + ".index"), StandardOpenOption.READ) else null

  private[this] final val cache = LeastRecentlyUsedCache[Array[A]](maxcachesize)

  private[this] final val indexcache = LeastRecentlyUsedCache[Array[IndexType]](maxcachesize)

  private[this] final val mask = (1 << pagefactor) - 1

}

/**
 * pagefactor is n in 2 ^ n, for instance a pagefactor of 10 will result in 1024 entries per page
 */
final class MemoryMappedColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  name: String,

  capacity: IndexType,

  pagefactor: Int,

  maxcachesize: Int,

  filepath: String,

  ordering: Option[Ordering[A]])

  extends ColumnBuilder[A, MemoryMappedColumn[A]] {

  private type P = (A, Int)

  /**
   * A good starting point, but should be fine tuned with the default constructor.
   */
  final def this(name: String, capacity: IndexType, filepath: String, ordering: Option[Ordering[A]]) = this(name, capacity, 10, 1024, filepath, ordering)

  final def next(value: A): Unit = {
    val i = nextIndex
    array.update(i & mask, value)
    if (0 == (length & mask)) {
      val os = newStream(out)
      os.writeObject(array)
      os.flush
      offsets += file.length
    }
    if (ordering.isDefined) {
      if (0 == (i & chunkmask)) {
        ignore(chunk.close)
        chunk = output(chunkcount.incrementAndGet)._1
      }
      chunk.writeObject((value, i))
    }
  }

  final def get = {
    if (0 < (length & mask)) {
      val os = newStream(out)
      os.writeObject(array.take(length & mask))
      os.flush
      offsets += file.length
    }
    out.close
    if (ordering.isDefined) {
      ignore(chunk.close)
      mergeSort(chunkcount.get)
      workingdir.delete
    }
    new MemoryMappedColumn[A](name, length, pagefactor, maxcachesize, offsets.toArray, indexoffsets.toArray, filepath, ordering)
  }

  private[this] final def mergeSort(n: Int) = {
    implicit val pairordering = new Ordering[P] {
      @inline final def compare(a: P, b: P) = ordering.get.compare(a._1, b._1) match {
        case 0 ⇒ Ordering[Int].compare(a._2, b._2)
        case c ⇒ c
      }
    }

    def sort(range: Range, level: Int) = for (i ← range) {
      val (in, _) = input(i)
      val chunks =
        if (i < n) {
          Array.fill(chunkmask + 1)(in.readObject.asInstanceOf[P])
        } else {
          val a = new ArrayBuffer[P](chunkmask / 2)
          try while (true) a += in.readObject.asInstanceOf[P] catch { case e: EOFException ⇒ }
          a.toArray
        }
      in.close
      val (sortedchunk, _) = output(i)
      chunks.sorted.foreach(sortedchunk.writeObject)
      sortedchunk.close
    }

    @tailrec def mergeFiles(files: Int, level: Int): Unit = if (1 < files) {
      val r = files + (if (0 == files % 2) 0 else 1)
      chunkcount.set(0)
      if (4 < (r / 8)) {
        split8(1 to r, level, mergeChunks)
      } else if (2 < (r / 4)) {
        split4(1 to r, level, mergeChunks)
      } else if (1 < (r / 2)) {
        split2(1 to r, level, mergeChunks)
      } else {
        mergeChunks(1 to r, level)
      }
      mergeFiles(workingdir.listFiles.length, level + 1)
    }

    def mergeChunks(range: Range, level: Int): Unit = if (1 < range.length) {
      val prev = "_" * level
      val next = "_" * (level + 1)
      val (low, high) = range.splitAt(range.length / 2)
      low.zipAll(high, -1, -1).map {
        case (l, r) ⇒
          println(level + " : " + l + "+" + r)
          val (left, lfile) = try input(prev + l) catch { case _: Throwable ⇒ (null, null) }
          val (right, rfile) = try input(prev + r) catch { case _: Throwable ⇒ (null, null) }
          val (out, ofile) = output(next + chunkcount.incrementAndGet)
          if (null == lfile) {
            out.close
            rfile.renameTo(ofile)
          } else if (null == rfile) {
            out.close
            lfile.renameTo(ofile)
          } else {
            try merge(BufferedStream(left), BufferedStream(right), out)
            finally {
              lfile.delete
              rfile.delete
            }
          }
      }
    }

    @inline @tailrec def merge(left: BufferedStream, right: BufferedStream, out: ObjectOutputStream): Unit = {
      ((left.next, right.next) match {
        case (Some(x), Some(y)) ⇒ if (0 <= pairordering.compare(x, y)) left.consume else right.consume
        case (Some(_), None) ⇒ left.consume
        case (None, Some(_)) ⇒ right.consume
        case (None, None) ⇒ None
      }) match {
        case Some(value) ⇒
          out.writeObject(value)
          merge(left, right, out)
        case None ⇒
          left.close
          right.close
          out.close
      }
    }

    def unzipIndexFile = {
      val ofile = new File(filepath + ".index")
      val out = new FileOutputStream(ofile)
      val (in, ifile) = input(workingdir.listFiles.head)
      val array = new Array[IndexType](1 << pagefactor)
      var i = 0
      try while (true) {
        val a = in.readObject.asInstanceOf[P]
        array.update(i & mask, a._2)
        if (0 < i && 0 == (i & mask)) {
          val os = new ObjectOutputStream(out)
          os.writeObject(array)
          os.flush
          indexoffsets += ofile.length
        }
        i += 1
      } catch {
        case e: EOFException ⇒
      } finally {
        in.close
        ifile.delete
        i -= 1
      }
      if (0 < (i & mask)) {
        val os = new ObjectOutputStream(out)
        os.writeObject(array.take(i & mask))
        os.flush
        indexoffsets += ofile.length
      }
      out.close
    }

    def split2(range: Range, level: Int, body: (Range, Int) ⇒ Any) = {
      val (r1, r2) = range.splitAt(range.length / 2)
      val f2 = future(body(r2, level))
      body(r1, level)
      Await.ready(f2, Duration.Inf)
    }

    def split4(range: Range, level: Int, body: (Range, Int) ⇒ Any) = {
      val (l, r) = range.splitAt(range.length / 2)
      val (r1, r2) = l.splitAt(l.length / 2)
      val (r3, r4) = r.splitAt(r.length / 2)
      val f2 = future(body(r2, level))
      val f3 = future(body(r3, level))
      val f4 = future(body(r4, level))
      body(r1, level)
      Await.ready(f2, Duration.Inf)
      Await.ready(f3, Duration.Inf)
      Await.ready(f4, Duration.Inf)
    }

    def split8(range: Range, level: Int, body: (Range, Int) ⇒ Any) = {
      val (l, r) = range.splitAt(range.length / 2)
      val (l1, l2) = l.splitAt(l.length / 2)
      val (rr1, rr2) = r.splitAt(r.length / 2)
      val (r1, r2) = l1.splitAt(l1.length / 2)
      val (r3, r4) = l2.splitAt(l2.length / 2)
      val (r5, r6) = rr1.splitAt(rr1.length / 2)
      val (r7, r8) = rr2.splitAt(rr2.length / 2)
      val f2 = future(body(r2, level))
      val f3 = future(body(r3, level))
      val f4 = future(body(r4, level))
      val f5 = future(body(r5, level))
      val f6 = future(body(r6, level))
      val f7 = future(body(r7, level))
      val f8 = future(body(r8, level))
      body(r1, level)
      Await.ready(f2, Duration.Inf)
      Await.ready(f3, Duration.Inf)
      Await.ready(f4, Duration.Inf)
      Await.ready(f5, Duration.Inf)
      Await.ready(f6, Duration.Inf)
      Await.ready(f7, Duration.Inf)
      Await.ready(f8, Duration.Inf)
    }

    final case class BufferedStream(in: ObjectInputStream) {
      final def close = in.close
      final def next: Option[P] = {
        buffer match {
          case None ⇒ buffer = try Some(in.readObject.asInstanceOf[P]) catch { case e: EOFException ⇒ None }
          case _ ⇒
        }
        buffer
      }
      final def consume = { val b = buffer; buffer = None; b }
      private[this] final var buffer: Option[P] = None
    }

    split8(1 to n, 0, sort)
    mergeFiles(workingdir.listFiles.length, 0)
    unzipIndexFile
  }

  private[this] final def newStream(out: OutputStream) = new ObjectOutputStream(LZ4.newFastOutputStream(out))

  private[this] final def output(f: File) = (new ObjectOutputStream(LZ4.newFastOutputStream(new FileOutputStream(f))), f)

  private[this] final def output(suffix: Any): (ObjectOutputStream, File) = output(new File(workingdir.getAbsolutePath + "/chunk." + suffix))

  private[this] final def input(f: File) = (new ObjectInputStream(LZ4.newInputStream(new FileInputStream(f))), f)

  private[this] final def input(suffix: Any): (ObjectInputStream, File) = input(new File(workingdir.getAbsolutePath + "/chunk." + suffix))

  private[this] final var chunk: ObjectOutputStream = null

  private[this] final val array = new Array[A](1 << pagefactor)

  private[this] final val offsets = { val p = new ArrayBuffer[Long]; p += 0L; p }

  private[this] final val indexoffsets = { val p = new ArrayBuffer[Long]; p += 0L; p }

  private[this] final val file = new File(filepath)

  private[this] final val out = new FileOutputStream(file)

  private[this] final val chunkcount = new AtomicInteger

  private[this] final val workingdir = if (ordering.isDefined) io.temporaryDirectory else null

  private[this] final val mask = (1 << pagefactor) - 1

  private[this] final val chunkmask = (1 << (pagefactor + 7)) - 1

}
