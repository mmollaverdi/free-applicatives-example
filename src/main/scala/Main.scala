import java.time.LocalDateTime

import cats.{Applicative, Monad, ~>}
import cats.free.{Free, FreeApplicative}
import cats.syntax.cartesian._
import monix.eval.Task

import scala.concurrent.Await
import scala.concurrent.duration._

object Main {

  import Interpreters._

  implicit val scheduler = monix.execution.Scheduler.fixedPool("appThreadPool", 10)

  def main(args: Array[String])  {
    log("Starting")
    val task = ApplicativeScriptInterpreter.run[Int](App.apply())
    val result = Await.result(task.runAsync, 10.seconds)
    log(s"Result is $result")
    log("Finished")
  }
}

object TaskApplicativeInstance {
  implicit val TaskApplicative: Applicative[Task] = new Applicative[Task] {
    override def pure[A](x: A): Task[A] = Task.now(x)

    override def ap[A, B](ff: Task[A => B])(fa: Task[A]): Task[B] = Task.mapBoth(ff, fa)((f, a) => f(a))
  }
}

sealed trait Action[A]
case class Action1(input: Int) extends Action[Int]
case class Action2(input: Int) extends Action[Int]
case class Action3(input: Int) extends Action[Int]

object App {

  type MonadicScript[A] = Free[Action, A]
  type AppScript[A] = FreeApplicative[MonadicScript, A]

  def liftToApplicative[X](script: MonadicScript[X]): AppScript[X] = FreeApplicative.lift[MonadicScript, X](script)

  def apply(): AppScript[Int] = {
    val monadicComposition = Free.liftF(Action1(10)).flatMap(o1 => Free.liftF(Action2(o1)))
    val action3 = Free.liftF(Action3(10))

    val applicativeComposition =
      (liftToApplicative(monadicComposition) |@| liftToApplicative(action3)) map { _ + _ }
    applicativeComposition
  }
}

object Interpreters {
  import App.{AppScript, MonadicScript}

  def log(s: String) = println(s"${LocalDateTime.now()} - $s")

  object MonadicScriptInterpreter extends (Action ~> Task) {

    import monix.cats._
    implicitly[Monad[Task]]

    override def apply[A](action: Action[A]): Task[A] = action match {
      case Action1(i) => {
        Task.apply { log("Started Action1"); i + 1 }.map(x=> { log("Finished Action1"); x } )
      }
      case Action2(i) => {
        Task.apply { log("Started Action2"); i + 2 }.delayResult(5.seconds).map(x=> { log("Finished Action2"); x } )
      }
      case Action3(i) => {
        Task.apply { log("Started Action3"); i + 3 }.map(x=> { log("Finished Action3"); x } )
      }
    }

    def run[A](script: MonadicScript[A]): Task[A] = script.foldMap(this)
  }

  object ApplicativeScriptInterpreter extends (MonadicScript ~> Task) {

    import TaskApplicativeInstance._

    def run[A](script: AppScript[A]): Task[A] = {
      println(s"script $script")
      script.foldMap(this)
    }

    override def apply[A](script: MonadicScript[A]): Task[A] = MonadicScriptInterpreter.run(script)
  }
}


