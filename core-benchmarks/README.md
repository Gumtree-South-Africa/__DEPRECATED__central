# COMaaS Benchmarks

Create the benchmarks executable:

`mvn clean install -Pbenchmarks,-distribution -Drevision=1-SNAPSHOT`

Run the benchmarks executable:

`java -jar core-benchmarks/target/core-benchmarks-1-SNAPSHOT.jar`

Baseline results on a MacBook Pro (15-inch, 2017), 2,9 GHz Intel Core i7:

```
Benchmark                        Mode  Cnt         Score        Error  Units
XidFactoryBenchmark.guids       thrpt   20   4350190.699 ± 146011.589  ops/s
XidFactoryBenchmark.uuid        thrpt   20    550394.353 ±  11572.914  ops/s
XidFactoryBenchmark.xidFactory  thrpt   20  17795179.123 ± 544912.105  ops/s
XidFactoryBenchmark.uuid5       thrpt   20   9502501.825 ± 152469.086  ops/s
```