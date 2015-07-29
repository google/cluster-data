_John Wilkes and Charles Reiss._<br>Copyright © 2011,2014 Google Inc. All rights reserved.<br>
<br>
The <code>clusterdata-2011-2</code> trace represents 29 day's worth of cell information from May 2011, on a cluster of about 11k machines.  <b>To find out how to download it, and what it contains, read the trace-data  <a href='https://drive.google.com/open?id=0B5g07T_gRDg9Z0lsSTEtTWtpOW8&authuser=0'>v2.1 format + schema document</a>.</b>

<table><thead><th> If you haven't already joined our <a href='https://groups.google.com/forum/#!forum/googleclusterdata-discuss'>mailing list</a>, please do so now. If you haven't already filled out our <a href='http://goo.gl/GIDUh'>brief  survey</a>, please do so now. We'll use these to keep in touch with you, and announce goodies such as new traces. </th></thead><tbody>
<tr><td> As of 2014-11-18, the <code>clusterdata-2011-2</code> trace has replaced the <code>clusterdata-2011-1</code> trace.                                                                                                                                                                                                                            </td></tr></tbody></table>

The <code>clusterdata-2011-2</code> trace starts at 19:00 EDT on Sunday May 1, 2011, and the datacenter is in that timezone (US Eastern).  This corresponds to a trace timestamp of 600s; see the data schema documentation for why.<br>
<br>
<b>The <code>clusterdata-2011-2</code> trace is identical to the one called <code>clusterdata-2011-1</code> and described in <a href='ClusterData2011_1.md'>here</a>, except for the addition of a single new column of data in the <code>task_usage</code> tables.</b>
The new data is a randomly-picked 1 second sample of CPU usage from within the associated 5-minute usage-reporting period for that task.  Using this data, it will be possible to build up a stochastic model of task utilization over time for long-running tasks.<br>
<br>
<h1>Trace data</h1>

The trace is stored in <a href='https://developers.google.com/storage/'>Google Storage for Developers</a> in the bucket called <code>clusterdata-2011-2</code>.<br>
Most users will want the <a href='https://developers.google.com/storage/docs/gsutil'>gsutil</a> command-line tool to download the trace data.  <b>Download instructions are in the trace format + schema file linked-to above.</b>

The total size of the compressed trace is approximately 41GB.<br>
<br>
Priorities in this trace range from 0 to 11 inclusive; bigger numbers mean "more important". 0 and 1 are “free” priorities; 9, 10, and 11 and “production” priorities; and 10 is a “monitoring” priority.<br>
<br>
<h2>Known anomalies in the trace</h2>

Disk-time-fraction data is only included in about the first 14 days, because of a change in our monitoring system.<br>
<br>
Some jobs are deliberately omitted because they ran primarily on machines not included in this trace. The portion that ran on included machines amounts to approximately 0.003% of the machines’ task-seconds of usage.<br>
<br>
We are aware of only one example of a job that retains its job ID after being stopped, reconfigured, and restarted (job number 6253771429).<br>
<br>
Approximately 70 jobs (for example, job number 6377830001) have job event records but no task event records. We believe that  this is legitimate in a majority of cases: typically because the job is started but its tasks are disabled for its entire duration.<br>
<br>
Approximately 0.013% of task events and 0.0008% of job events in this trace have a non-empty missing info field.<br>
<br>
We estimate that less than 0.05% of job and task scheduling event records are missing and less than 1% of resource usage measurements are missing.<br>
<br>
Some cycles per instruction (CPI) and memory accesses per instruction (MAI) measurements are clearly inaccurate (for example, they are above or below the range possible on the underlying micro-architectures). We believe these measurements are caused by bugs in the data-capture system used, such as the cycle counter and instruction counter not being read at the same time. To obtain useful data from these measurements, we suggest filtering out measurements representing a very small amount of CPU time and measurements with unreasonable CPI and MAI values.<br>
<br>
<h1>Change log</h1>

<table><thead><th>2014-11-17</th><th> This page created. </th></thead><tbody></tbody></table>

