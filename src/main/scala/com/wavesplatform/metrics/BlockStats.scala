package com.wavesplatform.metrics

import java.net.InetSocketAddress

import org.influxdb.dto.Point
import scorex.block.{Block, MicroBlock}

object BlockStats {

  private val StringIdLength = 6

  sealed abstract class Event {
    val name: String = {
      val className = getClass.getName
      className.slice(className.lastIndexOf('$', className.length - 2) + 1, className.length - 1)
    }
  }

  object Event {
    case object Received extends Event
    case object Applied extends Event
    case object Declined extends Event
    case object Mined extends Event
  }

  def received(b: Block, from: InetSocketAddress): Unit = write(
    Point
      .measurement("block")
      .addField("id", b.uniqueId.toString.take(StringIdLength))
      .addField("from", from.toString)
      .addField("propTime", System.currentTimeMillis() - b.timestamp),
    Event.Received,
    Seq.empty
  )

  def applied(b: Block): Unit = write(
    Point
      .measurement("block")
      .addField("id", b.uniqueId.toString.take(StringIdLength))
      .addField("txs", b.transactionData.size),
    Event.Applied,
    Seq.empty
  )

  def declined(b: Block): Unit = write(
    Point
      .measurement("block")
      .addField("id", b.uniqueId.toString.take(StringIdLength)),
    Event.Declined,
    Seq.empty
  )

  def mined(b: Block): Unit = write(
    Point
      .measurement("block")
      .addField("id", b.uniqueId.toString.take(StringIdLength))
      .addField("parentId", b.reference.toString.take(StringIdLength))
      .addField("txs", b.transactionData.size)
      .addField("score", b.blockScore),
    Event.Mined,
    Seq.empty
  )


  def received(m: MicroBlock, from: InetSocketAddress, propagationTime: Long): Unit = write(
    Point
      .measurement("micro")
      .addField("id", m.uniqueId.toString.take(StringIdLength))
      .addField("parentId", m.prevResBlockSig.toString.take(StringIdLength))
      .addField("from", from.toString)
      .addField("propTime", propagationTime),
    Event.Received,
    Seq.empty
  )

  def applied(m: MicroBlock): Unit = write(
    Point
      .measurement("micro")
      .addField("id", m.uniqueId.toString.take(StringIdLength))
      .addField("txs", m.transactionData.size),
    Event.Applied,
    Seq.empty
  )

  def declined(m: MicroBlock): Unit = write(
    Point
      .measurement("micro")
      .addField("id", m.uniqueId.toString.take(StringIdLength)),
    Event.Declined,
    Seq.empty
  )

  def mined(m: MicroBlock): Unit = write(
    Point
      .measurement("micro")
      .addField("id", m.uniqueId.toString.take(StringIdLength))
      .addField("txs", m.transactionData.size),
    Event.Mined,
    Seq.empty
  )


  private def write(init: Point.Builder, event: Event, addFields: Seq[(String, String)]): Unit = {
    Metrics.write(addFields.foldLeft(init.addField("event", event.name)) { case (r, (k, v)) => r.addField(k, v) })
  }
}
