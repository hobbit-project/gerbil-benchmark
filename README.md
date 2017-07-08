# Gerbil Benchmark

This is an implementation of the GERBIL benchmark in Hobbit. 

The same metrics (micro/macro) Recall, Precision and F-Measure will be used.

# Links

[Gerbil](http://aksw.org/Projects/GERBIL.html)

[Gerbil Github](https://github.com/AKSW/gerbil)

# Systems

In `gerbil-systems`, a general adapter for all systems compatible to GERBIL is implemented. However, please note that some of the systems need an API key. This key has to be provided in `gerbil-systems/src/main/resources/gerbil_keys.properties`. If no keys should be used, an empty file should be provided.