scalaVersion := "2.11.8"

sbtVersion := "0.13.11"

name := "test-free-applicatives"

val catsVersion = "0.6.0"

val monixVersion = "2.0-RC8"

libraryDependencies ++= Seq(
  "org.typelevel"                   %% "cats-core"                   % catsVersion,
  "org.typelevel"                   %% "cats-free"                   % catsVersion,
  "io.monix"                        %% "monix"                       % monixVersion,
  "io.monix"                        %% "monix-cats"                  % monixVersion
)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xlint",
  "-language:higherKinds")

testOptions in Test ++= Seq(
  Tests.Argument("junitxml", "html", "console", "!pandoc"),
  Tests.Setup( () => System.setProperty("mode", "test"))
)

