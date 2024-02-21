# PowerData 2019 traces

The `powerdata-2019` trace dataset provides power utilization information for 57
power domains in Google data centers. Two of these power domains are from cells
in data centers with the new medium voltage power plane design. The remainder
belong to the eight cells featured in the
[2019 Cluster Data trace](ClusterData2019.md).

Please see [the documentation](power_trace_documentation.pdf) for details on
what's in this dataset and how to access it. For additional background, please
refer to the paper [Data Center Power Oversubscription with a Medium Voltage
Power Plane and Priority-Aware
Capping](https://research.google/pubs/data-center-power-oversubscription-with-a-medium-voltage-power-plane-and-priority-aware-capping/).

Also included is a [colab](power_trace_analysis_colab.ipynb) recreating the
figures as an example of how to query the data.

## Notes

If you use this data we'd appreciate if you cite the paper and let us know about
your work! The best way to do so is through the mailing list.

*   If you haven't already joined our
    [mailing list](https://groups.google.com/forum/#!forum/googleclusterdata-discuss),
    please do so now. *Important: to avoid spammers, you MUST fill out the
    "reason" field, or your application will be rejected.*

![Creative Commons CC-BY license](https://i.creativecommons.org/l/by/4.0/88x31.png)
The data and trace documentation are made available under the
[CC-BY](https://creativecommons.org/licenses/by/4.0/) license. By downloading it
or using them, you agree to the terms of this license.

**Questions?**

You can send email to googleclusterdata-discuss@googlegroups.com. The more
detailed the request the greater the chance that somebody can help you: screen
shots, concrete examples, error messages, and a list of what you already tried
are all useful.
