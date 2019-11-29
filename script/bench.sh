#!/usr/bin/env bash
mkdir logs
echo "bench,small-heap,parallel"
lein with-profile bench,small-heap,parallel run | tee logs/bench-small-parallel.log
echo "bench,med-heap,parallel"
lein with-profile bench,med-heap,parallel run | tee logs/bench-med-parallel.log
echo "bench,big-heap,parallel"
lein with-profile bench,big-heap,parallel run | tee logs/bench-big-parallel.log

echo "bench,small-heap,g1"
lein with-profile bench,small-heap,g1 run | tee logs/bench-small-g1.log
echo "bench,med-heap,g1"
lein with-profile bench,med-heap,g1 run | tee logs/bench-med-g1.log
echo "bench,big-heap,g1"
lein with-profile bench,big-heap,g1 run | tee logs/bench-big-g1.log
