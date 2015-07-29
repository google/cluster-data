_Authors: Jiri Simsa `<`[jsimsa@cs.cmu.edu](mailto:jsimsa@cs.cmu.edu)`>`, john wilkes `<`[johnwilkes@google.com](mailto:johnwilkes@google.com)`>`_<br>
Version of: 2012.09.26<br>
<br>
<h1>Introduction</h1>
<a href='http://www.pdl.cmu.edu/PDL-FTP/associated/CMU-PDL-11-113.pdf'>ETA</a> (Exploratory Testing Architecture) is a testing framework that explores the execution of a distributed application, looking for bugs that are provoked by particular sequences of events caused by non-determinism such as timing and asynchrony.  ETA has been developed for <a href='http://research.google.com/university/relations/facultysummit2011/'>Omega</a>, a new cluster management system at Google.<br>
<br>
As part of its functionality, ETA provides estimates for when its exploratory testing will finish. Achieving accurate runtime estimations is a significant research challenge, and so in order to stimulate interest, and foster research in improving these estimates, we have made available traces of a number of ETA’s real-world exploratory test runs.<br>
<br>
You can find the <a href='http://commondatastorage.googleapis.com/clusterdata-misc/ETA-traces.tar.gz'>traces here</a>.  (<a href='http://en.wikipedia.org/wiki/SHA-1#Data_Integrity'>SHA1 checksum</a>:	6664e43caa1bf1f4c0af959fe93d266ead24d234.)<br>
<br>
<h1>Format</h1>
These traces describe the execution tree structure explored by ETA. In short, the execution tree represents at abstract level the different sequences in which concurrent events can happen during an execution of a test. Further, ETA uses <a href='http://dl.acm.org/citation.cfm?id=1040315'>state space reduction</a> to avoid the need to explore equivalent sequences. In other words, certain parts of the execution tree may never be explored. For details on the execution tree structure and application of the state space reduction in ETA read our <a href='http://www.pdl.cmu.edu/PDL-FTP/associated/CMU-PDL-11-113.pdf'>technical report</a>.<br>
<br>
An exploration trace contains a sequence of events that detail the exploration. Each event can be one of the following:<br>
<br>
<ul><li><code>AddNode x y</code> -- A node <code>x</code> with parent <code>y</code> has been added (the parent of the root is -1).<br>
</li><li><code>Explore x</code> -- The node <code>x</code> has been marked for further exploration.<br>
</li><li><code>Transition x</code> -- The exploration transitioned from the current node to node <code>x</code>.<br>
</li><li><code>Start</code> -- New test execution (starting from node 0) has been initiated.<br>
</li><li><code>End t</code> -- Current test execution finished after <code>t</code> time units.</li></ul>

A well-formed trace contains a number of executions, each starting with <code>Start</code> and ending with <code>End t</code>. Each execution explores a branch of the execution tree, transitioning from the root all the way to a leaf (<code>Transition x</code>), and optionally adding newly encountered nodes to the tree (<code>AddNode x y</code>), and identifying which unvisited nodes should be explored in future (<code>Explore x</code>).<br>
<br>
<h1>Traces</h1>
These are all provided in a single compressed tar file (see above for the link).<br>
<br>
<ul><li><code>resource_X.trace</code>: The <code>resource_X</code> test is representative of a class of Omega tests that evaluate interactions of <code>X</code> different users that acquire and release resources from a pool of <code>X</code> resources.<br>
</li><li><code>store_X_Y_Z.trace</code>: The <code>store_X_Y_Z</code> test is representative of a class of Omega tests that evaluate interactions of X users of a distributed key-value store with <code>Y</code> front-end nodes and <code>Z</code> back-end nodes.<br>
</li><li><code>scheduling_X.trace</code>: The <code>scheduling_X</code> test is representative of a class of Omega tests that evaluate interactions of <code>X</code> users issuing concurrent scheduling requests.<br>
</li><li><code>tlp.trace</code>: The <code>tlp</code> test is representative of a class of Omega tests that do scheduling work.</li></ul>

<h1>Notes</h1>
The data may be freely used for any purpose, although acknowledgement of Google as the source of the data would be appreciated, and we’d love to be sent copies of any papers you publish that use it.<br>
<br>
Questions?  Send email to the authors (listed at the top of this page).<br>
<br>
