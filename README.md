stixar-graphlib
===============

Mavenized version of [stixar graph-library](https://code.google.com/p/stixar-graphlib/). Transitive closure with Nuutila's algorithm was modified to support [Lucene's OpenBitSet](http://lucene.apache.org/core/4_0_0/core/org/apache/lucene/util/OpenBitSet.html). This modifications enables the computation of the transitive closure for chains of length greater than sqrt(Integer.MAX_VALUE).
This is a requirement for [Inferray](https://github.com/jsubercaze/inferray/). However since it is possible to go up to sqrt(Long.MAX_VALUE) beware to limit the size of the chain regarding the available memory unless you want a good'ol [OutOfMemoryError](http://docs.oracle.com/javase/7/docs/api/java/lang/OutOfMemoryError.html) to take place.

 
Original project on Google Code : https://code.google.com/p/stixar-graphlib/

Note that this graph library is an underdog among the large list of graph library. It contains several very efficient and well implemented algorithms.

### Use with Maven

First you need to setup the server in your pom.xml :


    <repositories>
      <repository>
        <id>stixar-mvn-repo</id>
        <url>https://raw.github.com/jsubercaze/stixar-graphlib/mvn-repo/</url>
      </repository>
    </repositories>

Then use the following dependency :

    <dependency>
      <groupId>com.stixar</groupId>
      <artifactId>graphlib</artifactId>
      <version>0.0.2</version>
    </dependency>

