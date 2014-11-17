name := "reading-service-common"

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "com.typesafe.akka"         %%  "akka-slf4j"            % akkaV,
    "com.blinkbox.books"        %%  "common-json"           % "0.2.3",
    "com.blinkbox.books"        %%  "common-lang"           % "0.2.0",
    "com.blinkbox.books"        %%  "common-slick"          % "0.3.1",
    "com.blinkbox.books"        %%  "common-spray"          % "0.19.1",
    "com.blinkbox.books"        %%  "common-spray-auth"     % "0.7.4",
    "com.blinkbox.books"        %%  "common-scala-test"     % "0.3.0"   % Test,
    "com.typesafe.slick"        %%  "slick"                 % "2.1.0",
    "io.spray"                  %%  "spray-testkit"         % sprayV    % Test
  )
}
