# ClusterData 2019 traces

_John Wilkes._

The `clusterdata-2019` trace dataset provides information about eight different Borg cells for the month of May 2019.  It includes the following new information:

 * CPU usage information histograms for each 5 minute period, not just a point sample;
 * information about alloc sets (shared resource reservations used by jobs);
 * job-parent information for master/worker relationships such as MapReduce jobs.

The 2019 traces focus on resource requests and usage, and contain no information about end users, their data, or access patterns to storage systems and other services.

Because of it's size (about 2.4TiB compressed), we are only making the trace data available via [Google BigQuery](https://cloud.google.com/bigquery) so that sophisticated analyses can be performed without requiring local resources. 

**The `clusterdata-2019` traces are described in this document:
[Google cluster-usage traces v3](https://drive.google.com/file/d/10r6cnJ5cJ89fPWCgj7j4LtLBqYN9RiI9/view).**  You can find the download and access instructions there, as well as many more details about what is in the traces, and how to interpret them. For additional background information, please refer to the 2015 Borg paper, [Large-scale cluster management at Google with Borg](https://ai.google/research/pubs/pub43438). 

  * If you haven't already joined our
    [mailing list](https://groups.google.com/forum/#!forum/googleclusterdata-discuss),
    please do so now.
    *Important: to avoid spammers, you MUST fill out the "reason" field, or your application will be rejected.*

![Creative Commons CC-BY license](https://i.creativecommons.org/l/by/4.0/88x31.png)
The data and trace documentation are made available under the
[CC-BY](https://creativecommons.org/licenses/by/4.0/) license.
By downloading it or using them, you agree to the terms of this license.

**Questions?**

You can send email to googleclusterdata-discuss@googlegroups.com.  The more detailed the request the greater the chance that somebody can help you: screen shots, concrete examples, error messages, and a list of what you already tried are all useful.

**Acknowledgements**

This trace is the result of a collaboration involving Muhammad Tirmazi, Nan Deng, Md Ehtesam Haque, Zhijing Gene Qin, Steve Hand and Adam Barker.
