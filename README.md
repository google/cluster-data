# Overview

This repository describes various traces from parts of the Google cluster
management software and systems.

*   Please join our (low volume)
    [discussion group](http://groups.google.com/group/googleclusterdata-discuss),
    so we can send you announcements, and you can let us know about any issues,
    insights, or papers you publish using these traces. **Important: to avoid
    spammers, you MUST fill out the "reason" field, or your application will be
    rejected**. Once you are a member, you can send email to
    [googleclusterdata-discuss@googlegroups.com](mailto:googleclusterdata-discuss@googlegroups.com)
    to:

    *   Announce tools and techniques that can help others analyze or decode the
        trace data.
    *   Share insights and surprises.
    *   Ask questions (the group has a few hundred members) and get help. If you
        ask for help, please include concrete examples of issues you run into;
        screen shots; error codes; and a list of what you have already tried.
        Don't just say "I can't download the data"!

*   We provide a **[trace bibliography](bibliography.bib)** of papers that have
    used and/or analyzed the traces, and encourage anybody who publishes one to
    add it to the bibliography using a github pull request [preferred], or by
    emailing the bibtex entry to
    [googleclusterdata-discuss@googlegroups.com](mailto:googleclusterdata-discuss@googlegroups.com).
    In either case, please mimic the existing format **exactly**.

# Borg cluster workload traces

These are traces of workloads running on Google compute cells that are managed
by the cluster management software internally known as Borg.

*   **[version 3](ClusterData2019.md)** (aka `ClusterData2019`) provides data
    from eight Borg cells over the month of May 2019.
*   [version 2](ClusterData2011_2.md) (aka `ClusterData2011`) provides data from
    a single 12.5k-machine Borg cell from May 2011.
*   [version 1](TraceVersion1.md) is an older, short trace that describes a 7
    hour period from one cell from 2009. *Deprecated. We strongly recommend
    using the version 2 or version 3 traces instead.*

## ETA traces

In addition, this site hosts a set of
[execution traces from ETA](ETAExplorationTraces.md) (Exploratory Testing
Architecture) - a testing framework that explores interactions between
distributed, concurrently-executing components, with an eye towards improving
testing them.

## Power traces

This site also hosts [power traces](PowerData2019.md) for 57 power domains
during the month of May 2019. This trace is synergistic with the
`ClusterData2019` dataset.

## License

![Creative Commons CC-BY license](https://i.creativecommons.org/l/by/4.0/88x31.png)
The data and trace documentation are made available under the
[CC-BY](https://creativecommons.org/licenses/by/4.0/) license. By downloading it
or using them, you agree to the terms of this license.
