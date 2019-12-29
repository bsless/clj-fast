# Benchmarks Results

## Test Methods

### Running tests

```bash
lein with-profile bench,small-heap,parallel run
```

### Benchmarks framework

[Criterium](https://github.com/hugoduncan/criterium) is used to run quick benchmarks.

### Profiles

Benchmarks are run on all combinations of the following:

- heap size: big (9G), medium (5G), small (2G).
- Garbage collection: parallel, G1

## Tests details

### assoc

Assoc and fast assoc performance are tested with maps and records.

### assoc-in

Assoc-in is tested vs. an inlined implementation with vanilla maps, gets and 
assoc, all core functions.

### get

`get` was tested on `map`, `record` and `fast-map`, `fast-get` was tested on `fast-map`.

Moreover, different get methods were tested:
- map on keyword.
- keyword on map.
- keyword on record.
- `.get` from record.
- `.field` from record.

### merge

#### Fast map merge

fast map merge was implemented by Metosin and uses `kv-reduce` to assoc
one map into another.
Was compared vs. regular merge.

#### Inline Merge

Two different implementations of inlining merge were tested, one based on the core implementation of merge, and one on Metosin's.

### get-in

`get-in` was tested against an inlined implementation.

### select-keys

`select-keys` was tested against an inlined implementation.

## Results Summary

### assoc

NOTE: the first result is surprising as it does not correlate with benchmarks
run in the REPL. Requires further study.

- `assoc` to record ~twice as fast as `assoc`ing to map.
- `fast-assoc` ~ 5.7% faster than `assoc`. (Metosin)

### Assoc-in

- the inlined implementation is always faster and exhibits compounding returns 
for deeper maps.

### get

NOTE: Same case as the assoc experiment, with benchmarks results being the opposite of what's measured in a running REPL.

- `get` from record ~50% slower than from map.
- `fast-get` from `fast-map` ~ 8.4% faster than `get`ting from regular map. (Metosin)
- map on keyword > keyword on map > get from map (ordered by speed)
- field from record > keyword on record > .get from record > get from record.

### merge

#### Fast Map merge

Distinctly faster than regular merge, by about 30%, but differences might depend on map sizes, so more benchmarks are required.

#### Inline merge & fast merge

Sees diminishing returns on the benefit of merging more maps, but the speedup
is measurable.

### get-in

Inline implementation faster by a factor of 4-5.

### select-keys

Inline implementation faster by a factor of 10 or more, depends
on the number of selected keys.

| heap  | gc       | test                     | execution time mean |
|-------|----------|--------------------------|---------------------|
| big   | g1       | assoc to map             | 59.332289 ns        |
| big   | g1       | assoc to record          | 37.817791 ns        |
| big   | g1       | fast-assoc to map        | 58.745566 ns        |
| big   | g1       | fast-assoc to record     | 38.130746 ns        |
| big   | g1       | assoc-in 1               | 55.401622 ns        |
| big   | g1       | assoc-in 2               | 80.624996 ns        |
| big   | g1       | assoc-in 3               | 192.783355 ns       |
| big   | g1       | assoc-in 4               | 213.392817 ns       |
| big   | g1       | inline-assoc-in 1        | 34.242111 ns        |
| big   | g1       | inline-assoc-in 2        | 47.701248 ns        |
| big   | g1       | inline-assoc-in 3        | 56.055236 ns        |
| big   | g1       | inline-assoc-in 4        | 64.356245 ns        |
| big   | g1       | get from map             | 12.062397 ns        |
| big   | g1       | map on keyword           | 9.588496 ns         |
| big   | g1       | keyword on map           | 10.667132 ns        |
| big   | g1       | get from record          | 19.981760 ns        |
| big   | g1       | keyword on record        | 6.742613 ns         |
| big   | g1       | .get from record         | 12.936960 ns        |
| big   | g1       | get field from record    | 4.459152 ns         |
| big   | g1       | get from fast-map        | 39.422209 ns        |
| big   | g1       | fast-get from fast-map   | 10.350487 ns        |
| big   | g1       | merge maps               | 475.631317 ns       |
| big   | g1       | fast merge maps          | 339.311945 ns       |
| big   | g1       | merge 2 maps             | 498.986602 ns       |
| big   | g1       | inline merge 2 maps      | 410.048586 ns       |
| big   | g1       | inline fast merge 2 maps | 340.333425 ns       |
| big   | g1       | merge 3 maps             | 1.740212 µs         |
| big   | g1       | inline merge 3 maps      | 1.527734 µs         |
| big   | g1       | inline fast merge 3 maps | 1.514455 µs         |
| big   | g1       | merge 4 maps             | 2.077769 µs         |
| big   | g1       | inline merge 4 maps      | 1.872643 µs         |
| big   | g1       | inline fast merge 4 maps | 1.878031 µs         |
| big   | g1       | get-in 1                 | 41.409732 ns        |
| big   | g1       | fast get-in 1            | 8.635020 ns         |
| big   | g1       | get-in 2                 | 55.447390 ns        |
| big   | g1       | fast get-in 2            | 13.116437 ns        |
| big   | g1       | get-in 3                 | 75.028493 ns        |
| big   | g1       | fast get-in 3            | 19.523285 ns        |
| big   | g1       | get-in 4                 | 86.881961 ns        |
| big   | g1       | fast get-in 4            | 23.385800 ns        |
| big   | g1       | select 1/4 keys          | 190.425200 ns       |
| big   | g1       | fast select 1/4 keys     | 22.999439 ns        |
| big   | g1       | select 2/4 keys          | 272.499804 ns       |
| big   | g1       | fast select 2/4 keys     | 29.172286 ns        |
| big   | g1       | select 3/4 keys          | 366.475217 ns       |
| big   | g1       | fast select 3/4 keys     | 39.829664 ns        |
| big   | g1       | select 4/4 keys          | 434.643118 ns       |
| big   | g1       | fast select 4/4 keys     | 54.022224 ns        |
| big   | parallel | assoc to map             | 32.928582 ns        |
| big   | parallel | assoc to record          | 20.446087 ns        |
| big   | parallel | fast-assoc to map        | 29.502707 ns        |
| big   | parallel | fast-assoc to record     | 16.348120 ns        |
| big   | parallel | assoc-in 1               | 58.213462 ns        |
| big   | parallel | assoc-in 2               | 80.355196 ns        |
| big   | parallel | assoc-in 3               | 174.212831 ns       |
| big   | parallel | assoc-in 4               | 195.459019 ns       |
| big   | parallel | inline-assoc-in 1        | 39.048452 ns        |
| big   | parallel | inline-assoc-in 2        | 119.447055 ns       |
| big   | parallel | inline-assoc-in 3        | 132.599956 ns       |
| big   | parallel | inline-assoc-in 4        | 132.870844 ns       |
| big   | parallel | get from map             | 10.527692 ns        |
| big   | parallel | map on keyword           | 8.198648 ns         |
| big   | parallel | keyword on map           | 10.579735 ns        |
| big   | parallel | get from record          | 13.530892 ns        |
| big   | parallel | keyword on record        | 5.337923 ns         |
| big   | parallel | .get from record         | 11.568563 ns        |
| big   | parallel | get field from record    | 3.241501 ns         |
| big   | parallel | get from fast-map        | 35.473335 ns        |
| big   | parallel | fast-get from fast-map   | 8.567708 ns         |
| big   | parallel | merge maps               | 347.655705 ns       |
| big   | parallel | fast merge maps          | 235.524543 ns       |
| big   | parallel | merge 2 maps             | 400.498928 ns       |
| big   | parallel | inline merge 2 maps      | 290.172027 ns       |
| big   | parallel | inline fast merge 2 maps | 236.766179 ns       |
| big   | parallel | merge 3 maps             | 1.202006 µs         |
| big   | parallel | inline merge 3 maps      | 1.065885 µs         |
| big   | parallel | inline fast merge 3 maps | 1.030146 µs         |
| big   | parallel | merge 4 maps             | 1.537560 µs         |
| big   | parallel | inline merge 4 maps      | 1.327976 µs         |
| big   | parallel | inline fast merge 4 maps | 1.299183 µs         |
| big   | parallel | get-in 1                 | 37.481520 ns        |
| big   | parallel | fast get-in 1            | 9.600175 ns         |
| big   | parallel | get-in 2                 | 53.096389 ns        |
| big   | parallel | fast get-in 2            | 17.359211 ns        |
| big   | parallel | get-in 3                 | 71.010664 ns        |
| big   | parallel | fast get-in 3            | 17.586213 ns        |
| big   | parallel | get-in 4                 | 85.462583 ns        |
| big   | parallel | fast get-in 4            | 21.161095 ns        |
| big   | parallel | select 1/4 keys          | 174.924109 ns       |
| big   | parallel | fast select 1/4 keys     | 11.400825 ns        |
| big   | parallel | select 2/4 keys          | 223.177093 ns       |
| big   | parallel | fast select 2/4 keys     | 17.823062 ns        |
| big   | parallel | select 3/4 keys          | 273.173577 ns       |
| big   | parallel | fast select 3/4 keys     | 24.636845 ns        |
| big   | parallel | select 4/4 keys          | 325.571896 ns       |
| big   | parallel | fast select 4/4 keys     | 34.395232 ns        |
| med   | g1       | assoc to map             | 51.046722 ns        |
| med   | g1       | assoc to record          | 28.271085 ns        |
| med   | g1       | fast-assoc to map        | 48.819286 ns        |
| med   | g1       | fast-assoc to record     | 27.344378 ns        |
| med   | g1       | assoc-in 1               | 45.567260 ns        |
| med   | g1       | assoc-in 2               | 72.605293 ns        |
| med   | g1       | assoc-in 3               | 184.430012 ns       |
| med   | g1       | assoc-in 4               | 205.577025 ns       |
| med   | g1       | inline-assoc-in 1        | 23.347550 ns        |
| med   | g1       | inline-assoc-in 2        | 34.121930 ns        |
| med   | g1       | inline-assoc-in 3        | 44.075392 ns        |
| med   | g1       | inline-assoc-in 4        | 56.272411 ns        |
| med   | g1       | get from map             | 8.766992 ns         |
| med   | g1       | map on keyword           | 5.940280 ns         |
| med   | g1       | keyword on map           | 7.684307 ns         |
| med   | g1       | get from record          | 11.302393 ns        |
| med   | g1       | keyword on record        | 4.094054 ns         |
| med   | g1       | .get from record         | 8.798486 ns         |
| med   | g1       | get field from record    | 2.804539 ns         |
| med   | g1       | get from fast-map        | 35.960625 ns        |
| med   | g1       | fast-get from fast-map   | 6.341502 ns         |
| med   | g1       | merge maps               | 449.240905 ns       |
| med   | g1       | fast merge maps          | 330.673556 ns       |
| med   | g1       | merge 2 maps             | 491.076049 ns       |
| med   | g1       | inline merge 2 maps      | 404.880242 ns       |
| med   | g1       | inline fast merge 2 maps | 328.919549 ns       |
| med   | g1       | merge 3 maps             | 1.656626 µs         |
| med   | g1       | inline merge 3 maps      | 1.500121 µs         |
| med   | g1       | inline fast merge 3 maps | 1.482626 µs         |
| med   | g1       | merge 4 maps             | 2.082713 µs         |
| med   | g1       | inline merge 4 maps      | 1.859986 µs         |
| med   | g1       | inline fast merge 4 maps | 1.897681 µs         |
| med   | g1       | get-in 1                 | 35.452887 ns        |
| med   | g1       | fast get-in 1            | 7.104161 ns         |
| med   | g1       | get-in 2                 | 49.642836 ns        |
| med   | g1       | fast get-in 2            | 11.944550 ns        |
| med   | g1       | get-in 3                 | 71.148620 ns        |
| med   | g1       | fast get-in 3            | 11.756572 ns        |
| med   | g1       | get-in 4                 | 86.056836 ns        |
| med   | g1       | fast get-in 4            | 14.471972 ns        |
| med   | g1       | select 1/4 keys          | 184.251997 ns       |
| med   | g1       | fast select 1/4 keys     | 11.546493 ns        |
| med   | g1       | select 2/4 keys          | 261.605759 ns       |
| med   | g1       | fast select 2/4 keys     | 17.688072 ns        |
| med   | g1       | select 3/4 keys          | 335.078348 ns       |
| med   | g1       | fast select 3/4 keys     | 28.244369 ns        |
| med   | g1       | select 4/4 keys          | 411.665584 ns       |
| med   | g1       | fast select 4/4 keys     | 36.746129 ns        |
| med   | parallel | assoc to map             | 32.816854 ns        |
| med   | parallel | assoc to record          | 21.455291 ns        |
| med   | parallel | fast-assoc to map        | 30.972880 ns        |
| med   | parallel | fast-assoc to record     | 15.767671 ns        |
| med   | parallel | assoc-in 1               | 66.722851 ns        |
| med   | parallel | assoc-in 2               | 97.666359 ns        |
| med   | parallel | assoc-in 3               | 204.360843 ns       |
| med   | parallel | assoc-in 4               | 230.833788 ns       |
| med   | parallel | inline-assoc-in 1        | 111.571909 ns       |
| med   | parallel | inline-assoc-in 2        | 119.389995 ns       |
| med   | parallel | inline-assoc-in 3        | 129.400426 ns       |
| med   | parallel | inline-assoc-in 4        | 135.195647 ns       |
| med   | parallel | get from map             | 10.260191 ns        |
| med   | parallel | map on keyword           | 7.574712 ns         |
| med   | parallel | keyword on map           | 9.385637 ns         |
| med   | parallel | get from record          | 13.551867 ns        |
| med   | parallel | keyword on record        | 5.297737 ns         |
| med   | parallel | .get from record         | 11.230381 ns        |
| med   | parallel | get field from record    | 2.830761 ns         |
| med   | parallel | get from fast-map        | 36.519058 ns        |
| med   | parallel | fast-get from fast-map   | 8.445671 ns         |
| med   | parallel | merge maps               | 354.773666 ns       |
| med   | parallel | fast merge maps          | 220.719467 ns       |
| med   | parallel | merge 2 maps             | 385.602278 ns       |
| med   | parallel | inline merge 2 maps      | 286.700270 ns       |
| med   | parallel | inline fast merge 2 maps | 218.298030 ns       |
| med   | parallel | merge 3 maps             | 1.229720 µs         |
| med   | parallel | inline merge 3 maps      | 1.057462 µs         |
| med   | parallel | inline fast merge 3 maps | 1.007392 µs         |
| med   | parallel | merge 4 maps             | 1.543486 µs         |
| med   | parallel | inline merge 4 maps      | 1.402021 µs         |
| med   | parallel | inline fast merge 4 maps | 1.273665 µs         |
| med   | parallel | get-in 1                 | 37.119941 ns        |
| med   | parallel | fast get-in 1            | 9.277836 ns         |
| med   | parallel | get-in 2                 | 52.930622 ns        |
| med   | parallel | fast get-in 2            | 17.178237 ns        |
| med   | parallel | get-in 3                 | 71.649230 ns        |
| med   | parallel | fast get-in 3            | 23.609074 ns        |
| med   | parallel | get-in 4                 | 88.262548 ns        |
| med   | parallel | fast get-in 4            | 19.807807 ns        |
| med   | parallel | select 1/4 keys          | 191.446694 ns       |
| med   | parallel | fast select 1/4 keys     | 11.325831 ns        |
| med   | parallel | select 2/4 keys          | 273.711523 ns       |
| med   | parallel | fast select 2/4 keys     | 20.313217 ns        |
| med   | parallel | select 3/4 keys          | 347.366049 ns       |
| med   | parallel | fast select 3/4 keys     | 22.696730 ns        |
| med   | parallel | select 4/4 keys          | 358.466666 ns       |
| med   | parallel | fast select 4/4 keys     | 33.401705 ns        |
| small | g1       | assoc to map             | 50.370181 ns        |
| small | g1       | assoc to record          | 26.768723 ns        |
| small | g1       | fast-assoc to map        | 47.003602 ns        |
| small | g1       | fast-assoc to record     | 26.669350 ns        |
| small | g1       | assoc-in 1               | 44.903473 ns        |
| small | g1       | assoc-in 2               | 72.325635 ns        |
| small | g1       | assoc-in 3               | 172.828045 ns       |
| small | g1       | assoc-in 4               | 203.929691 ns       |
| small | g1       | inline-assoc-in 1        | 22.354323 ns        |
| small | g1       | inline-assoc-in 2        | 30.154590 ns        |
| small | g1       | inline-assoc-in 3        | 37.755347 ns        |
| small | g1       | inline-assoc-in 4        | 46.933515 ns        |
| small | g1       | get from map             | 7.041507 ns         |
| small | g1       | map on keyword           | 5.964055 ns         |
| small | g1       | keyword on map           | 13.276343 ns        |
| small | g1       | get from record          | 11.637279 ns        |
| small | g1       | keyword on record        | 5.162029 ns         |
| small | g1       | .get from record         | 8.686448 ns         |
| small | g1       | get field from record    | 3.253523 ns         |
| small | g1       | get from fast-map        | 36.473490 ns        |
| small | g1       | fast-get from fast-map   | 6.825633 ns         |
| small | g1       | merge maps               | 461.178224 ns       |
| small | g1       | fast merge maps          | 327.707789 ns       |
| small | g1       | merge 2 maps             | 494.381186 ns       |
| small | g1       | inline merge 2 maps      | 417.167723 ns       |
| small | g1       | inline fast merge 2 maps | 337.355828 ns       |
| small | g1       | merge 3 maps             | 1.701648 µs         |
| small | g1       | inline merge 3 maps      | 1.541413 µs         |
| small | g1       | inline fast merge 3 maps | 1.512119 µs         |
| small | g1       | merge 4 maps             | 1.995103 µs         |
| small | g1       | inline merge 4 maps      | 1.838894 µs         |
| small | g1       | inline fast merge 4 maps | 1.916917 µs         |
| small | g1       | get-in 1                 | 34.880021 ns        |
| small | g1       | fast get-in 1            | 7.502044 ns         |
| small | g1       | get-in 2                 | 53.651392 ns        |
| small | g1       | fast get-in 2            | 9.234544 ns         |
| small | g1       | get-in 3                 | 70.019072 ns        |
| small | g1       | fast get-in 3            | 11.914789 ns        |
| small | g1       | get-in 4                 | 85.144262 ns        |
| small | g1       | fast get-in 4            | 15.094950 ns        |
| small | g1       | select 1/4 keys          | 183.335504 ns       |
| small | g1       | fast select 1/4 keys     | 12.141344 ns        |
| small | g1       | select 2/4 keys          | 262.677577 ns       |
| small | g1       | fast select 2/4 keys     | 19.374445 ns        |
| small | g1       | select 3/4 keys          | 336.513424 ns       |
| small | g1       | fast select 3/4 keys     | 30.052434 ns        |
| small | g1       | select 4/4 keys          | 422.432701 ns       |
| small | g1       | fast select 4/4 keys     | 39.221278 ns        |
| small | parallel | assoc to map             | 33.008467 ns        |
| small | parallel | assoc to record          | 20.817475 ns        |
| small | parallel | fast-assoc to map        | 30.826503 ns        |
| small | parallel | fast-assoc to record     | 16.092035 ns        |
| small | parallel | assoc-in 1               | 55.631617 ns        |
| small | parallel | assoc-in 2               | 83.062750 ns        |
| small | parallel | assoc-in 3               | 186.146620 ns       |
| small | parallel | assoc-in 4               | 214.371304 ns       |
| small | parallel | inline-assoc-in 1        | 38.039322 ns        |
| small | parallel | inline-assoc-in 2        | 44.933155 ns        |
| small | parallel | inline-assoc-in 3        | 52.564147 ns        |
| small | parallel | inline-assoc-in 4        | 56.862140 ns        |
| small | parallel | get from map             | 9.088907 ns         |
| small | parallel | map on keyword           | 7.462205 ns         |
| small | parallel | keyword on map           | 8.489695 ns         |
| small | parallel | get from record          | 12.298008 ns        |
| small | parallel | keyword on record        | 5.546769 ns         |
| small | parallel | .get from record         | 9.974411 ns         |
| small | parallel | get field from record    | 3.091465 ns         |
| small | parallel | get from fast-map        | 35.169612 ns        |
| small | parallel | fast-get from fast-map   | 7.824765 ns         |
| small | parallel | merge maps               | 355.172816 ns       |
| small | parallel | fast merge maps          | 225.698704 ns       |
| small | parallel | merge 2 maps             | 387.508335 ns       |
| small | parallel | inline merge 2 maps      | 282.705428 ns       |
| small | parallel | inline fast merge 2 maps | 220.856816 ns       |
| small | parallel | merge 3 maps             | 1.197984 µs         |
| small | parallel | inline merge 3 maps      | 1.036725 µs         |
| small | parallel | inline fast merge 3 maps | 985.635652 ns       |
| small | parallel | merge 4 maps             | 1.564001 µs         |
| small | parallel | inline merge 4 maps      | 1.262949 µs         |
| small | parallel | inline fast merge 4 maps | 1.248962 µs         |
| small | parallel | get-in 1                 | 37.240187 ns        |
| small | parallel | fast get-in 1            | 8.231005 ns         |
| small | parallel | get-in 2                 | 59.387337 ns        |
| small | parallel | fast get-in 2            | 13.672582 ns        |
| small | parallel | get-in 3                 | 72.205593 ns        |
| small | parallel | fast get-in 3            | 17.660160 ns        |
| small | parallel | get-in 4                 | 76.566448 ns        |
| small | parallel | fast get-in 4            | 22.120572 ns        |
| small | parallel | select 1/4 keys          | 187.972608 ns       |
| small | parallel | fast select 1/4 keys     | 11.570107 ns        |
| small | parallel | select 2/4 keys          | 239.516919 ns       |
| small | parallel | fast select 2/4 keys     | 16.343722 ns        |
| small | parallel | select 3/4 keys          | 287.652327 ns       |
| small | parallel | fast select 3/4 keys     | 28.252189 ns        |
| small | parallel | select 4/4 keys          | 431.191645 ns       |
| small | parallel | fast select 4/4 keys     | 34.818244 ns        |
