name          := """scala-textboard"""
version       := "0.0.1"
scalaVersion  := "2.11.8"
scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

libraryDependencies ++=  Seq(
			 "com.typesafe.akka" 	 %%    "akka-stream" 						  % "2.4.12",
			 "com.typesafe.akka" 	 %%    "akka-http-core" 					  % "2.4.11",
			 "com.typesafe.akka" 	 %%    "akka-http-experimental" 			  % "2.4.11",
             "com.typesafe.akka"     %%    "akka-http-testkit"        			  % "2.4.11"	%    "test",
             "com.typesafe.akka"     %%    "akka-http-spray-json-experimental"    % "2.4.11",
             "org.postgresql"        %     "postgresql"               			  % "9.4-1206-jdbc41",
             "org.scalactic" 		 %%    "scalactic" 							  % "3.0.0",
             "org.scalatest"         %%    "scalatest"    	          			  % "3.0.0"     %    "test",
			 "org.slf4j" 			 % 	   "slf4j-simple" 						  % "1.7.21",
             "com.typesafe.slick"    %%    "slick"            	      			  % "3.1.1",
             "com.typesafe.slick"    %%    "slick-hikaricp"           			  % "3.1.1",
			 "com.typesafe.slick" 	 % 	   "slick-testkit_2.11" 				  % "3.1.1",
             "com.typesafe.akka"     %%    "akka-actor"                           % "2.4.12",
			 "com.wix" %% "accord-core" % "0.6"

)

fork in run := true

connectInput in run := true