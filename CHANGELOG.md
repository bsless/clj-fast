# Change Log

## Unreleased - 2020-02-10

### Major changes

- Reorganize namespaces and file structure:
  - clj-fast
    - core: low level functions operating directly on Clojure data
      structures and Metosin functions
    - inline: inline implementations of core Clojure functions
    - lens: macro implementations of lenses
    - util: utility functions used as by macros.
    - collections
      - hash-map: functions to operate directly on hash maps
      - concurrent-hash-map: functions to operate directly on concurrent hash map
- Add slow benchmarking option to benchmarks namespaces.

## [0.0.4-alpha] - 2020-02-01

### Add

- memoize implementations and benchmarks

## [0.0.3-alpha] - 2020-01-31

### Major changes

- Complete rewrite of benchmark suit.
- Complete rewrite of results details.

### Add

- transient implementation for merge (Joinr)

## [0.0.2-alpha] - 2020-01-01

### Add

- more profile cases
- pre check to inline-update-in
- Usage in README

### Remove

- fast-get-in-th
- defrec->inline-select-keys and its utility functions

### Rename

- fast-select-keys-inline to inline-select-keys

## [0.0.1-alpha] - 2019-12-31

### Initial alpha release
