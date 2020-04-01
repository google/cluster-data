
|*Note: for new work, we **strongly** recommend using the [version 2](ClusterData2011_2.md) or [version 3](ClusterData2019.md) traces, which are more recent, more comprehensive, and provide much more data.* |
|:--------- |


_This trace was first announced in [this January 2010 blog post](https://ai.googleblog.com/2010/01/google-cluster-data.html)._

The first dataset provides traces from a Borg cell that were taken over a 7 hour
period. The workload consists of a set of tasks, where each task runs on a
single machine. Tasks consume memory and one or more cores (in fractional
units). Each task belongs to a single job; a job may have multiple tasks (e.g.,
mappers and reducers).

The trace data is available
[here](http://commondatastorage.googleapis.com/clusterdata-misc/google-cluster-data-1.csv.gz).
([SHA1 checksum](http://en.wikipedia.org/wiki/SHA-1#Data_Integrity):
98c87f059aa1cc37f1e9523ac691ee0fd5629188.)

The data have been anonymized in several ways: there are no task or job names,
just numeric identifiers; timestamps are relative to the start of data
collection; the consumption of CPU and memory is obscured using a linear
transformation. However, even with these transformations of the data,
researchers will be able to do workload characterizations (up to a linear
transformation of the true workload) and workload generation.

The data are structured as blank-separated columns. Each row reports on the
execution of a single task during a five minute period.

  * `Time` (int) - time in seconds since the start of data collection
  * `JobID` (int) - Unique identifier of the job to which this task belongs (**may be called ParentID**)
  * `TaskID` (int) - Unique identifier of the executing task
  * `Job Type` (0, 1, 2, 3) - class of job (a categorization of work)
  * `Normalized Task Cores` (float) - normalized value of the average number of cores used by the task
  * `Normalized Task Memory` (float) - normalized value of the average memory consumed by the task

![Creative Commons CC-BY license](https://i.creativecommons.org/l/by/4.0/88x31.png)
The data and trace documentation are made available under the
[CC-BY](https://creativecommons.org/licenses/by/4.0/) license.
By downloading it or using them, you agree to the terms of this license.

Questions? Send [email](mailto:googleclusterdata-discuss@googlegroups.com)
or peruse the 
[discussion group](http://groups.google.com/group/googleclusterdata-discuss).
