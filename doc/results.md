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

### get

`get` was tested on `map`, `record` and `fast-map`, `fast-get` was tested on `fast-map`.

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


### get

NOTE: Same case as the assoc experiment, with benchmarks results being the opposite of what's measured in a running REPL.

- `get` from record ~50% slower than from map.
- `fast-get` from `fast-map` ~ 8.4% faster than `get`ting from regular map. (Metosin)

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
| big   | g1       | assoc to map             | 61.113236 ns        |
| big   | g1       | assoc to record          | 36.828432 ns        |
| big   | g1       | fast-assoc to map        | 58.754356 ns        |
| big   | g1       | fast-assoc to record     | 36.853542 ns        |
| big   | g1       | get from map             | 11.254120 ns        |
| big   | g1       | get from record          | 15.916688 ns        |
| big   | g1       | get from fast-map        | 34.639626 ns        |
| big   | g1       | fast-get from fast-map   | 8.398670 ns         |
| big   | g1       | merge maps               | 468.518879 ns       |
| big   | g1       | fast merge maps          | 337.044207 ns       |
| big   | g1       | merge 2 maps             | 488.225915 ns       |
| big   | g1       | inline merge 2 maps      | 413.263303 ns       |
| big   | g1       | inline fast merge 2 maps | 331.951817 ns       |
| big   | g1       | merge 3 maps             | 1.649107 µs         |
| big   | g1       | inline merge 3 maps      | 1.498700 µs         |
| big   | g1       | inline fast merge 3 maps | 1.469241 µs         |
| big   | g1       | merge 4 maps             | 1.982242 µs         |
| big   | g1       | inline merge 4 maps      | 1.799967 µs         |
| big   | g1       | inline fast merge 4 maps | 1.852189 µs         |
| big   | g1       | get-in 1                 | 47.321596 ns        |
| big   | g1       | fast get-in 1            | 11.091595 ns        |
| big   | g1       | get-in 2                 | 62.515774 ns        |
| big   | g1       | fast get-in 2            | 17.281130 ns        |
| big   | g1       | get-in 3                 | 81.709551 ns        |
| big   | g1       | fast get-in 3            | 23.984918 ns        |
| big   | g1       | get-in 4                 | 94.852758 ns        |
| big   | g1       | fast get-in 4            | 31.363205 ns        |
| big   | g1       | select 1/4 keys          | 192.805338 ns       |
| big   | g1       | fast select 1/4 keys     | 24.008573 ns        |
| big   | g1       | select 2/4 keys          | 269.009349 ns       |
| big   | g1       | fast select 2/4 keys     | 32.971846 ns        |
| big   | g1       | select 3/4 keys          | 341.285199 ns       |
| big   | g1       | fast select 3/4 keys     | 42.623406 ns        |
| big   | g1       | select 4/4 keys          | 417.725270 ns       |
| big   | g1       | fast select 4/4 keys     | 55.718222 ns        |
| big   | parallel | assoc to map             | 30.336250 ns        |
| big   | parallel | assoc to record          | 15.148405 ns        |
| big   | parallel | fast-assoc to map        | 28.612822 ns        |
| big   | parallel | fast-assoc to record     | 14.223970 ns        |
| big   | parallel | get from map             | 8.577455 ns         |
| big   | parallel | get from record          | 12.991712 ns        |
| big   | parallel | get from fast-map        | 29.870261 ns        |
| big   | parallel | fast-get from fast-map   | 7.858593 ns         |
| big   | parallel | merge maps               | 320.763397 ns       |
| big   | parallel | fast merge maps          | 213.801150 ns       |
| big   | parallel | merge 2 maps             | 350.122623 ns       |
| big   | parallel | inline merge 2 maps      | 273.055883 ns       |
| big   | parallel | inline fast merge 2 maps | 219.514630 ns       |
| big   | parallel | merge 3 maps             | 1.117836 µs         |
| big   | parallel | inline merge 3 maps      | 996.941313 ns       |
| big   | parallel | inline fast merge 3 maps | 967.513389 ns       |
| big   | parallel | merge 4 maps             | 1.402239 µs         |
| big   | parallel | inline merge 4 maps      | 1.232263 µs         |
| big   | parallel | inline fast merge 4 maps | 1.229546 µs         |
| big   | parallel | get-in 1                 | 35.329460 ns        |
| big   | parallel | fast get-in 1            | 9.337990 ns         |
| big   | parallel | get-in 2                 | 53.094068 ns        |
| big   | parallel | fast get-in 2            | 10.847565 ns        |
| big   | parallel | get-in 3                 | 69.783249 ns        |
| big   | parallel | fast get-in 3            | 16.530730 ns        |
| big   | parallel | get-in 4                 | 83.155383 ns        |
| big   | parallel | fast get-in 4            | 20.386126 ns        |
| big   | parallel | select 1/4 keys          | 163.978379 ns       |
| big   | parallel | fast select 1/4 keys     | 10.057898 ns        |
| big   | parallel | select 2/4 keys          | 209.495863 ns       |
| big   | parallel | fast select 2/4 keys     | 15.536302 ns        |
| big   | parallel | select 3/4 keys          | 257.684443 ns       |
| big   | parallel | fast select 3/4 keys     | 23.050187 ns        |
| big   | parallel | select 4/4 keys          | 313.336666 ns       |
| big   | parallel | fast select 4/4 keys     | 31.287005 ns        |
| med   | g1       | assoc to map             | 45.976070 ns        |
| med   | g1       | assoc to record          | 20.416762 ns        |
| med   | g1       | fast-assoc to map        | 46.616263 ns        |
| med   | g1       | fast-assoc to record     | 25.896974 ns        |
| med   | g1       | get from map             | 6.806355 ns         |
| med   | g1       | get from record          | 10.711427 ns        |
| med   | g1       | get from fast-map        | 30.365640 ns        |
| med   | g1       | fast-get from fast-map   | 6.583860 ns         |
| med   | g1       | merge maps               | 433.511969 ns       |
| med   | g1       | fast merge maps          | 322.899167 ns       |
| med   | g1       | merge 2 maps             | 471.333021 ns       |
| med   | g1       | inline merge 2 maps      | 392.670811 ns       |
| med   | g1       | inline fast merge 2 maps | 326.465833 ns       |
| med   | g1       | merge 3 maps             | 1.697041 µs         |
| med   | g1       | inline merge 3 maps      | 1.532228 µs         |
| med   | g1       | inline fast merge 3 maps | 1.516973 µs         |
| med   | g1       | merge 4 maps             | 2.042337 µs         |
| med   | g1       | inline merge 4 maps      | 1.816936 µs         |
| med   | g1       | inline fast merge 4 maps | 1.850177 µs         |
| med   | g1       | get-in 1                 | 35.676935 ns        |
| med   | g1       | fast get-in 1            | 7.116657 ns         |
| med   | g1       | get-in 2                 | 51.302351 ns        |
| med   | g1       | fast get-in 2            | 12.300912 ns        |
| med   | g1       | get-in 3                 | 71.771528 ns        |
| med   | g1       | fast get-in 3            | 18.291218 ns        |
| med   | g1       | get-in 4                 | 86.642301 ns        |
| med   | g1       | fast get-in 4            | 14.479901 ns        |
| med   | g1       | select 1/4 keys          | 176.532454 ns       |
| med   | g1       | fast select 1/4 keys     | 11.138244 ns        |
| med   | g1       | select 2/4 keys          | 253.908699 ns       |
| med   | g1       | fast select 2/4 keys     | 17.962061 ns        |
| med   | g1       | select 3/4 keys          | 327.359457 ns       |
| med   | g1       | fast select 3/4 keys     | 27.035221 ns        |
| med   | g1       | select 4/4 keys          | 410.271550 ns       |
| med   | g1       | fast select 4/4 keys     | 37.981408 ns        |
| med   | parallel | assoc to map             | 30.201673 ns        |
| med   | parallel | assoc to record          | 14.871616 ns        |
| med   | parallel | fast-assoc to map        | 27.196398 ns        |
| med   | parallel | fast-assoc to record     | 13.986674 ns        |
| med   | parallel | get from map             | 8.307771 ns         |
| med   | parallel | get from record          | 12.706873 ns        |
| med   | parallel | get from fast-map        | 28.276485 ns        |
| med   | parallel | fast-get from fast-map   | 8.548348 ns         |
| med   | parallel | merge maps               | 322.278781 ns       |
| med   | parallel | fast merge maps          | 203.270152 ns       |
| med   | parallel | merge 2 maps             | 341.591746 ns       |
| med   | parallel | inline merge 2 maps      | 267.099709 ns       |
| med   | parallel | inline fast merge 2 maps | 208.388395 ns       |
| med   | parallel | merge 3 maps             | 1.118419 µs         |
| med   | parallel | inline merge 3 maps      | 994.581396 ns       |
| med   | parallel | inline fast merge 3 maps | 959.486032 ns       |
| med   | parallel | merge 4 maps             | 1.406699 µs         |
| med   | parallel | inline merge 4 maps      | 1.221757 µs         |
| med   | parallel | inline fast merge 4 maps | 1.210160 µs         |
| med   | parallel | get-in 1                 | 35.019328 ns        |
| med   | parallel | fast get-in 1            | 8.814005 ns         |
| med   | parallel | get-in 2                 | 52.345580 ns        |
| med   | parallel | fast get-in 2            | 16.352097 ns        |
| med   | parallel | get-in 3                 | 71.015159 ns        |
| med   | parallel | fast get-in 3            | 23.261005 ns        |
| med   | parallel | get-in 4                 | 83.307510 ns        |
| med   | parallel | fast get-in 4            | 19.905461 ns        |
| med   | parallel | select 1/4 keys          | 159.221260 ns       |
| med   | parallel | fast select 1/4 keys     | 9.341288 ns         |
| med   | parallel | select 2/4 keys          | 205.247535 ns       |
| med   | parallel | fast select 2/4 keys     | 15.684404 ns        |
| med   | parallel | select 3/4 keys          | 248.231714 ns       |
| med   | parallel | fast select 3/4 keys     | 24.595231 ns        |
| med   | parallel | select 4/4 keys          | 294.449093 ns       |
| med   | parallel | fast select 4/4 keys     | 30.842832 ns        |
| small | g1       | assoc to map             | 44.114516 ns        |
| small | g1       | assoc to record          | 20.496610 ns        |
| small | g1       | fast-assoc to map        | 43.817474 ns        |
| small | g1       | fast-assoc to record     | 23.992041 ns        |
| small | g1       | get from map             | 6.145270 ns         |
| small | g1       | get from record          | 10.446559 ns        |
| small | g1       | get from fast-map        | 28.535200 ns        |
| small | g1       | fast-get from fast-map   | 6.130083 ns         |
| small | g1       | merge maps               | 412.498729 ns       |
| small | g1       | fast merge maps          | 305.025975 ns       |
| small | g1       | merge 2 maps             | 455.026719 ns       |
| small | g1       | inline merge 2 maps      | 367.279979 ns       |
| small | g1       | inline fast merge 2 maps | 310.293840 ns       |
| small | g1       | merge 3 maps             | 1.558245 µs         |
| small | g1       | inline merge 3 maps      | 1.442795 µs         |
| small | g1       | inline fast merge 3 maps | 1.385871 µs         |
| small | g1       | merge 4 maps             | 1.884263 µs         |
| small | g1       | inline merge 4 maps      | 1.694471 µs         |
| small | g1       | inline fast merge 4 maps | 1.710964 µs         |
| small | g1       | get-in 1                 | 39.620108 ns        |
| small | g1       | fast get-in 1            | 7.554090 ns         |
| small | g1       | get-in 2                 | 49.442630 ns        |
| small | g1       | fast get-in 2            | 12.073545 ns        |
| small | g1       | get-in 3                 | 67.963449 ns        |
| small | g1       | fast get-in 3            | 17.609219 ns        |
| small | g1       | get-in 4                 | 81.463681 ns        |
| small | g1       | fast get-in 4            | 22.450452 ns        |
| small | g1       | select 1/4 keys          | 174.683094 ns       |
| small | g1       | fast select 1/4 keys     | 11.595299 ns        |
| small | g1       | select 2/4 keys          | 247.062633 ns       |
| small | g1       | fast select 2/4 keys     | 19.002476 ns        |
| small | g1       | select 3/4 keys          | 322.856196 ns       |
| small | g1       | fast select 3/4 keys     | 26.788163 ns        |
| small | g1       | select 4/4 keys          | 400.971945 ns       |
| small | g1       | fast select 4/4 keys     | 37.519308 ns        |
| small | parallel | assoc to map             | 30.559915 ns        |
| small | parallel | assoc to record          | 18.440237 ns        |
| small | parallel | fast-assoc to map        | 29.182437 ns        |
| small | parallel | fast-assoc to record     | 15.317732 ns        |
| small | parallel | get from map             | 8.121661 ns         |
| small | parallel | get from record          | 12.776023 ns        |
| small | parallel | get from fast-map        | 29.734204 ns        |
| small | parallel | fast-get from fast-map   | 7.735057 ns         |
| small | parallel | merge maps               | 319.205700 ns       |
| small | parallel | fast merge maps          | 169.451789 ns       |
| small | parallel | merge 2 maps             | 340.684097 ns       |
| small | parallel | inline merge 2 maps      | 268.477196 ns       |
| small | parallel | inline fast merge 2 maps | 166.656775 ns       |
| small | parallel | merge 3 maps             | 1.099006 µs         |
| small | parallel | inline merge 3 maps      | 965.866296 ns       |
| small | parallel | inline fast merge 3 maps | 933.204850 ns       |
| small | parallel | merge 4 maps             | 1.355002 µs         |
| small | parallel | inline merge 4 maps      | 1.196923 µs         |
| small | parallel | inline fast merge 4 maps | 1.161537 µs         |
| small | parallel | get-in 1                 | 34.593876 ns        |
| small | parallel | fast get-in 1            | 8.478306 ns         |
| small | parallel | get-in 2                 | 48.940654 ns        |
| small | parallel | fast get-in 2            | 15.078371 ns        |
| small | parallel | get-in 3                 | 66.426761 ns        |
| small | parallel | fast get-in 3            | 15.124507 ns        |
| small | parallel | get-in 4                 | 77.934975 ns        |
| small | parallel | fast get-in 4            | 18.981123 ns        |
| small | parallel | select 1/4 keys          | 162.602320 ns       |
| small | parallel | fast select 1/4 keys     | 10.699589 ns        |
| small | parallel | select 2/4 keys          | 203.301940 ns       |
| small | parallel | fast select 2/4 keys     | 15.931536 ns        |
| small | parallel | select 3/4 keys          | 249.721216 ns       |
| small | parallel | fast select 3/4 keys     | 23.322767 ns        |
| small | parallel | select 4/4 keys          | 294.004449 ns       |
| small | parallel | fast select 4/4 keys     | 29.192648 ns        |
