package cr.pulsar

import cats.{ Eq, Inject }
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import cr.pulsar.Config._
import fs2.Stream
import munit.FunSuite
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import scala.concurrent.ExecutionContext

class PulsarSpec extends FunSuite {

  implicit val `⏳` = IO.contextShift(ExecutionContext.global)
  implicit val `⏰` = IO.timer(ExecutionContext.global)

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
          case ioa: IO[_] => IO.suspend(ioa).unsafeToFuture
        })

  case class Event(value: String)

  object Event {
    implicit val eq: Eq[Event] = Eq.fromUniversalEquals

    implicit val inject: Inject[Event, Array[Byte]] =
      new Inject[Event, Array[Byte]] {
        def inj: Event => Array[Byte]         = _.value.getBytes("UTF-8")
        def prj: Array[Byte] => Option[Event] = bs => Event(new String(bs, "UTF-8")).some
      }
  }

  val cfg = Config(
    PulsarTenant("public"),
    PulsarNamespace("default"),
    PulsarURL("pulsar://localhost:6650")
  )
  val subs  = Subscription(Subscription.Name("test"), Subscription.Type.Failover)
  val spos  = SubscriptionInitialPosition.Latest
  val topic = Topic(cfg, Topic.Name("test"), Topic.Type.Persistent)
  val batch = Publisher.Batching.Disabled

  test("A message is published and consumed successfully") {
    val res: Resource[IO, (Consumer[IO], Publisher[IO, Event])] =
      for {
        client <- PulsarClient.create[IO](cfg.serviceUrl)
        consumer <- Consumer.create[IO](client, topic, subs, spos)
        blocker <- Blocker[IO]
        publisher <- Publisher.create[IO, Event](client, topic, batch, blocker)
      } yield consumer -> publisher

    Deferred[IO, Event].flatMap { latch =>
      Stream
        .resource(res)
        .flatMap {
          case (consumer, publisher) =>
            val consume =
              consumer.subscribe
                .through(Consumer.messageDecoder[IO, Event](consumer))
                .evalMap(latch.complete(_))

            val testEvent = Event("test")

            val produce =
              Stream(testEvent)
                .covary[IO]
                .evalMap(publisher.publish)
                .evalMap(_ => latch.get)

            produce.concurrently(consume).evalMap { e =>
              IO(assert(e === testEvent))
            }
        }
        .compile
        .drain
    }
  }

}