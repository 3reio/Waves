package com.wavesplatform.network

import com.wavesplatform.TransactionGen
import com.wavesplatform.state2.ByteStr
import io.netty.channel.embedded.EmbeddedChannel
import org.mockito.Mockito
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FreeSpec, Matchers}
import scorex.account.PublicKeyAccount
import scorex.block.MicroBlock
import scorex.transaction.NgHistory

import scala.concurrent.duration.DurationInt

class MicroBlockSynchronizerSpec extends FreeSpec
  with Matchers
  with MockFactory
  with PropertyChecks
  with Eventually
  with GeneratorDrivenPropertyChecks
  with TransactionGen {

  private implicit val pc: PatienceConfig = PatienceConfig(
    timeout = 1.second,
    interval = 50.millis
  )

  "should request next block" in {
    val lastBlockSig = ByteStr("lastBlockId".getBytes)
    val nextBlockSig = ByteStr("nextBlockId".getBytes)

    val history = Mockito.mock(classOf[NgHistory])
    Mockito.doReturn(Some(lastBlockSig)).when(history).lastBlockId()

    val channel = new EmbeddedChannel(new MicroBlockSynchronizer(history))
    channel.writeInbound(MicroBlockInv(nextBlockSig, lastBlockSig, System.currentTimeMillis()))
    channel.flushInbound()

    val r = eventually {
      val request = channel.readOutbound[MicroBlockRequest]()
      Option(request) shouldBe defined
      request
    }
    r shouldBe MicroBlockRequest(nextBlockSig)
  }

  "should not request the same block if it received before" in {
    val lastBlockSig = ByteStr("lastBlockId".getBytes)
    val nextBlockSig = ByteStr("nextBlockId".getBytes)

    val history = Mockito.mock(classOf[NgHistory])
    Mockito.doReturn(Some(lastBlockSig)).when(history).lastBlockId()

    val synchronizer = new MicroBlockSynchronizer(history)

    val channel1 = new EmbeddedChannel(synchronizer)
    val channel2 = new EmbeddedChannel(synchronizer)

    channel1.writeInbound(MicroBlockInv(nextBlockSig, lastBlockSig, System.currentTimeMillis()))
    channel1.flushInbound()

    eventually {
      val request = channel1.readOutbound[MicroBlockRequest]()
      Option(request) shouldBe defined
      request
    }

    channel1.writeInbound(MicroBlockResponse(MicroBlock(
      version = 1.toByte,
      generator = PublicKeyAccount("pubkey".getBytes),
      transactionData = Seq.empty,
      prevResBlockSig = lastBlockSig,
      totalResBlockSig = nextBlockSig,
      signature = nextBlockSig
    )))
    channel1.flushInbound()

    channel2.writeInbound(MicroBlockInv(nextBlockSig, lastBlockSig, System.currentTimeMillis()))
    channel2.flushInbound()

    intercept[TestFailedDueToTimeoutException] {
      eventually {
        val request = channel2.readOutbound[MicroBlockRequest]()
        Option(request) shouldBe defined
      }
    }
  }

}
