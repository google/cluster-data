# Overview

This repository describes various traces from parts of the Google cluster
management software and systems.

Please let us know about any issues, insights, or papers you publish using these
traces by
sending [email](mailto:googleclusterdata-discuss@googlegroups.com)
to the
[discussion group](http://groups.google.com/group/googleclusterdata-discuss).
(And please join this group to be kept up to date with new announcements!)
The more specific the data, the more likely we are to be able to help you.

If you have (or generate) tools that help analyze or decode the trace data, or
useful analyses, do please share them with this community.

A **[trace bibliography](bibliography.bib)** provides bibtex data for papers
about or derived from these traces.  If you publish one, please
[email](mailto:googleclusterdata-discuss@googlegroups.com) a bibtex entry for
it, so it can be added to the bibliography.  (Try to mimic the format used there
as exactly as possible.)

# Cluster workload traces
These are traces of workloads running on Google compute cells.

  * **[ClusterData2011\_2](ClusterData2011_2.md)** provides data from an
    12.5k-machine cell over about a month-long period in May 2011.
  * [TraceVersion1](TraceVersion1.md) is an older, short trace that describes a
    7 hour period from one cell (cluster).  *Deprecated. For new work, we
    recommend using the [ClusterData2011\_2](ClusterData2011_2.md) trace instead.*

# ETA traces

These are [execution traces from ETA](ETAExplorationTraces.md) (Exploratory
Testing Architecture) - a testing framework that explores interactions
between distrinbuted, concurrently-executing components.
