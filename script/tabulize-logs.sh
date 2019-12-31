#!/usr/bin/env bash
cd logs && grep -E "(## )|mean" *.log | grep -vE "## get-in ##|Update|select keys|vs|Assoc|## get ##" | sed -e 's/.log:/,/' -e 's/bench-//'| cut -f2 -d: | tr -d "#" | paste  - - -d, | sed 's/-/,/'  > results.csv
