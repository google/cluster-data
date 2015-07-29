# Introduction

[ETA](http://www.pdl.cmu.edu/PDL-FTP/associated/CMU-PDL-11-113.pdf) (Exploratory
Testing Architecture) is a testing framework that explores the execution of a
distributed application, looking for bugs that are provoked by particular
sequences of events caused by non-determinism such as timing and asynchrony.
ETA was developed for [Omega](http://research.google.com/pubs/pub41684.html), a
cluster management system developed at Google.

As part of its functionality, ETA provides estimates for when its exploratory
testing will finish. Achieving accurate runtime estimations is a significant
research challenge, and so in order to stimulate interest, and foster research
in improving these estimates, we have made available traces of a number of ETA’s
real-world exploratory test runs.

You can find the traces
[here](http://commondatastorage.googleapis.com/clusterdata-misc/ETA-traces.tar.gz).
([SHA1 checksum](http://en.wikipedia.org/wiki/SHA-1#Data_Integrity):
6664e43caa1bf1f4c0af959fe93d266ead24d234.)

# Format

These traces describe the execution tree structure explored by ETA. In short,
the execution tree represents at abstract level the different sequences in which
concurrent events can happen during an execution of a test. Further, ETA uses
[state space reduction](http://dl.acm.org/citation.cfm?id=1040315) to avoid the
need to explore equivalent sequences. In other words, certain parts of the
execution tree may never be explored. For details on the execution tree
structure and application of the state space reduction in ETA read our
[technical report](http://www.pdl.cmu.edu/PDL-FTP/associated/CMU-PDL-11-113.pdf).

An exploration trace contains a sequence of events that detail the
exploration. Each event can be one of the following:

 * `AddNode x y` -- a node `x` with parent `y` has been added (the parent of the root is -1)
 * `Explore x` -- the node `x` has been marked for further exploration
 * `Transition x` -- the exploration transitioned from the current node to node `x`
 * `Start` -- new test execution (starting from node 0) has been initiated
 * `End t` -- current test execution finished after `t` time units.

A well-formed trace contains a number of executions, each starting with `Start`
and ending with `End t`. Each execution explores a branch of the execution tree,
transitioning from the root all the way to a leaf (`Transition x`), and
optionally adding newly encountered nodes to the tree (`AddNode x y`), and
identifying which unvisited nodes should be explored in future (`Explore x`).

# Traces
These are all provided in a single compressed tar file (see above for the link).

 * `resource_X.trace`: The `resource_X` test is representative of a class of
   Omega tests that evaluate interactions of `X` different users that acquire
   and release resources from a pool of `X` resources.
 * `store_X_Y_Z.trace`: The `store_X_Y_Z` test is representative of a class of
   Omega tests that evaluate interactions of X users of a distributed key-value
   store with `Y` front-end nodes and `Z` back-end nodes.
 * `scheduling_X.trace`: The `scheduling_X` test is representative of a class of
   Omega tests that evaluate interactions of `X` users issuing concurrent
   scheduling requests.
 * `tlp.trace`: The `tlp` test is representative of a class of Omega tests that
   do scheduling work.

# Notes

The data may be freely used for any purpose, although acknowledgement of Google
as the source of the data would be appreciated, and we’d love to be sent copies
of any papers you publish that use it.

Questions?  Send us email!

  Jiri Simsa jsimsa@google.com, john wilkes johnwilkes@google.com

--------------------------------------

_Version of: 2012-09-26; revised 2015-07-29_
