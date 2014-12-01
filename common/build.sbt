name := "reading-service-common"

libraryDependencies ++= {
  val sprayV = "1.3.2"
  Seq(
    "com.blinkbox.books"        %%  "common-json"           % "0.2.4",
    "com.blinkbox.books"        %%  "common-lang"           % "0.2.1",
    "com.blinkbox.books"        %%  "common-slick"          % "0.3.2",
    "com.blinkbox.books"        %%  "common-spray"          % "0.23.0",
    "com.blinkbox.books"        %%  "common-spray-auth"     % "0.7.5",
    "com.blinkbox.books"        %%  "common-scala-test"     % "0.3.0"   % Test,
    "com.typesafe.slick"        %%  "slick"                 % "2.1.0",
    "io.spray"                  %%  "spray-testkit"         % sprayV    % Test
  )
}
