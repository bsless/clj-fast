# Change Log

## [Unreleased] - 2020-02-15

### Add

- basic tests, so we can sleep better.

### Changes and Improvements

- Relax the constraints on the possible values of keys during inline
  analysis. previously, only strings, keywords, symbols and integers
  were allowed, now all types are allowed. The only constraint is that
  the argument is a sequences or resolveable at compile time.
- Add bindings for arguments which look like sequences around `lens/put`
  and `lens/update`, as they appear more than once, and if they involve
  some heavy computation or side effects they should not be run more
  than once.
- Change / Fix how memoization worked for nullary functions. Now the
  result is just cached in an atom. Have to guard against a function
  returning `nil` with a sentinel value.

### Fix

- memoize nil values: Need to use sentinel values in the java maps to
  indicate nil return values from the invoked function.


## [0.0.5] - 2020-02-10

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
