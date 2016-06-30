import java.util.concurrent.TimeUnit

import cats.{Monad, ~>}
import cats.free.{Free, FreeApplicative}
import cats.syntax.cartesian._
import monix.eval.Task
import monix.cats._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  implicit val scheduler = monix.execution.Scheduler.fixedPool("appThreadPool", 10)

  def main(args: Array[String])  {
    println("Starting")
    val task = Interpreters.ApplicativeScriptInterpreter.run[Int](App.apply())
    val result = Await.result(task.runAsync, Duration(10, TimeUnit.SECONDS))
    println(s"Result is $result")
    println("Finished")
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

  implicitly[Monad[Task]]

  object MonadicScriptInterpreter extends (Action ~> Task) {
    override def apply[A](action: Action[A]): Task[A] = action match {
      case Action1(i) => Task.defer(Task.now(i + 1))
      case Action2(i) => Task.defer(Task.now(i + 2))
      case Action3(i) => Task.defer(Task.now(i + 3))
    }

    def run[A](script: MonadicScript[A]): Task[A] = script.foldMap(this)
  }

  object ApplicativeScriptInterpreter extends (MonadicScript ~> Task) {
    def run[A](script: AppScript[A]): Task[A] = script.foldMap(this)

    override def apply[A](script: MonadicScript[A]): Task[A] = MonadicScriptInterpreter.run(script)
  }
}


