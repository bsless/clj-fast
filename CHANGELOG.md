# Change Log

## [Unreleased]

### Add

- Variadic arity assoc-in. The analysis collapses all paths to a tree
  with leaves being the values to be assoc-ed and plans out a minimal
  execution. Also see [#23](https://github.com/bsless/clj-fast/issues/23)
- Not found arity to inline/get-in
- Tests from Clojure's test suit to catch some edge cases
- Faster update-in which takes advantage of variadic arities, but introduces ugly code duplication
- Faster versions of subseq/rsubseq which don't use sets for checking
  test functions identity.
- Static merge in `fast-map-merge`.
- `as` macro for annotating symbols.
- Box operations which mimic atom and volatile semantics.
- `update-in->` similar to update-in but takes many arguments.
- `kvreduce` - dispatches directly to `IKVReduce` `.kvreduce()` method instead of going through a protocol.
- Add `fast-count`.

### Fix

- inline/get not-found arity allowed any number of arguments, explicitly changed to one. [#24](https://github.com/bsless/clj-fast/issues/24)
- Fix callsite analysis of quoted forms. Now functions calls and quoted forms are handled correctly.
- inline/assoc-in new implementation did not extract bindings. Fixing
  this allows using side-effecting functions as keys

### Improve

- Relax the constraints in extract-bindings, making it less aggressive
  but still correct.
- Remove fn allocation in `fast-map-merge`.
- Change `fast-map-merge` to `definline`.

## [0.0.9]

- Add Circlci integration

## [0.0.8] - 2020-09-28

### Add

- Unrolled inline dissoc fo `& ks`
- Static merge case. If the map is written explicitly it can be
  `assoc`ed instead of `conj`ed. Closes
  [#6](https://github.com/bsless/clj-fast/issues/6)
- `dissoc-in`.
- `get` and `nth` macros.
- inline Clojure core implementations as drop in replacements.

## [0.0.7] - 2020-03-21

### Add

- Not found arity to core/val-at
- Not found arity to concurrent-map/get
- Correct inline implementation of `select-keys`.
- `map/get` which doesn't check input satisfies `Map`.

### Fix

- Broken test due to name change (memoize-h)
- rmerge not supporting collections not implementing IKVReduce (https://github.com/bsless/clj-fast/issues/1#issuecomment-582727080)

### Change

- Clean up type hints around collections wrappers.
- Rename `concurrent-hash-map?` -> `concurrent-map?`
- Rename `select-keys` -> `fast-select-keys`.
- Rename `map/get` -> `map/get?`.
- Use `doto` in `map/put`.

## [0.0.6] - 2020-02-22

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
