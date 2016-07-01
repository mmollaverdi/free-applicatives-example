# free-applicatives-example

Trying to figure out why I can't get actions composed via free applicatives and interpreted to Monix Tasks to run in parallel.

I have 2 actions which need to be executed sequentially, and another action which needs to be executed in parallel to the composition of those 2 actions:

```
|-action1-> -action2->
|-----action3----->
```

So action1 and action2 have monadic composition, but action3 is independent and needs to be composed with them through an applicative syntax in order to run in parallel in the interpreter.

I've used these types:

```
type MonadicScript[A] = Free[Action, A]
type AppScript[A] = FreeApplicative[MonadicScript, A]
```

So effectively, a free applicative of free of actions.
See the code [here](src/main/scala/Main.scala).

**But everything ends up running sequentially.**

I've delayed execution of action2 for 5 seconds and action 3 ends up only starting after 5 seconds too. I expect it to start immediate;y, in parallel to action1. 

This is the output I get for example:

```
2016-07-01T16:20:16.339 - Started Action1
2016-07-01T16:20:16.342 - Finished Action1
2016-07-01T16:20:16.359 - Started Action2
2016-07-01T16:20:21.370 - Finished Action2
2016-07-01T16:20:21.373 - Started Action3
2016-07-01T16:20:21.373 - Finished Action3
```

Replacing the monix-cats Monad instance with a custom Applicative instance though fixes the problem:

```
2016-07-01T16:33:36.790 - Started Action3
2016-07-01T16:33:36.790 - Finished Action3
2016-07-01T16:33:36.791 - Started Action1
2016-07-01T16:33:36.791 - Finished Action1
2016-07-01T16:33:36.799 - Started Action2
2016-07-01T16:33:41.805 - Finished Action2
```


