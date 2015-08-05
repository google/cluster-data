# ClusterData2011\_2 traces

_John Wilkes and Charles Reiss._

The `clusterdata-2011-2` trace represents 29 day's worth of cell information
from May 2011, on a cluster of about 12.5k machines.

  * If you haven't already joined our
    [mailing list](https://groups.google.com/forum/#!forum/googleclusterdata-discuss),
    please do so now.
  * If you haven't already filled out our [brief survey](http://goo.gl/GIDUh),
    please do so now. We use these to keep in touch with you, and announce
    goodies such as new traces.
  
## Trace data

The `clusterdata-2011-2` trace starts at 19:00 EDT on Sunday May 1, 2011, and
the datacenter is in that timezone (US Eastern).  This corresponds to a trace
timestamp of 600s; see the data schema documentation for why.

The trace is described in the trace-data
[v2.1 format + schema document](https://drive.google.com/open?id=0B5g07T_gRDg9Z0lsSTEtTWtpOW8&authuser=0).

Priorities in this trace range from 0 to 11 inclusive; bigger numbers mean "more
important". 0 and 1 are “free” priorities; 9, 10, and 11 and “production”
priorities; and 10 is a “monitoring” priority.<br>

The `clusterdata-2011-2` trace is identical to the one called
`clusterdata-2011-1`, except for the addition of a single new column of data in
the `task_usage` tables.  This new data is a randomly-picked 1 second sample of
CPU usage from within the associated 5-minute usage-reporting period for that
task.  Using this data, it is possible to build up a stochastic model of task
utilization over time for long-running tasks.

## Downloading the trace

Download instructions for the trace are in the
[v2.1 format + schema document](https://drive.google.com/open?id=0B5g07T_gRDg9Z0lsSTEtTWtpOW8&authuser=0).

The trace is stored in
[Google Storage for Developers](https://developers.google.com/storage/) in the
bucket called `clusterdata-2011-2`. The total size of the compressed trace is
approximately 41GB.

Most users should use the
[gsutil](https://developers.google.com/storage/docs/gsutil) command-line tool to
download the trace data.


## Known anomalies in the trace

Disk-time-fraction data is only included in about the first 14 days, because of
a change in our monitoring system.

Some jobs are deliberately omitted because they ran primarily on machines not
included in this trace. The portion that ran on included machines amounts to
approximately 0.003% of the machines’ task-seconds of usage.

We are aware of only one example of a job that retains its job ID after being
stopped, reconfigured, and restarted (job number 6253771429).

Approximately 70 jobs (for example, job number 6377830001) have job event
records but no task event records. We believe that this is legitimate in a
majority of cases: typically because the job is started but its tasks are
disabled for its entire duration.

Approximately 0.013% of task events and 0.0008% of job events in this trace have
a non-empty missing info field.

We estimate that less than 0.05% of job and task scheduling event records are
missing and less than 1% of resource usage measurements are missing.

Some cycles per instruction (CPI) and memory accesses per instruction (MAI)
measurements are clearly inaccurate (for example, they are above or below the
range possible on the underlying micro-architectures). We believe these
measurements are caused by bugs in the data-capture system used, such as the
cycle counter and instruction counter not being read at the same time. To obtain
useful data from these measurements, we suggest filtering out measurements
representing a very small amount of CPU time and measurements with unreasonable
CPI and MAI values.

# Questions?

Please send email to googleclusterdata-discuss@googlegroups.com.
