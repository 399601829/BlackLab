/*******************************************************************************
 * Copyright (c) 2010, 2012 Institute for Dutch Lexicology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package nl.inl.blacklab.search.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.resultproperty.HitProperty;
import nl.inl.blacklab.resultproperty.PropertyValue;
import nl.inl.blacklab.resultproperty.ResultProperty;
import nl.inl.blacklab.search.indexmetadata.Annotation;

/**
 * Groups results on the basis of a list of criteria, and provide random access
 * to the resulting groups.
 *
 * This implementation doesn't care in what order the spans appear, it will just
 * retrieve all of them and put each of them in a group. This takes more memory
 * and time than if the spans to be grouped are sequential (in which case you
 * should use ResultsGrouperSequential).
 */
public class HitGroupsImpl extends HitGroups {

    /**
     * The groups.
     */
    private Map<PropertyValue, HitGroup> groups = new HashMap<>();

    /**
     * Total number of hits.
     */
    private int totalHits = 0;

    /**
     * Size of the largest group.
     */
    private int largestGroupSize = 0;

    private WindowStats windowStats = null;

    private SampleParameters sampleParameters = null;
    
    /**
     * Construct a ResultsGrouper object, by grouping the supplied hits.
     *
     * NOTE: this will be made package-private in a future release. Use
     * Hits.groupedBy(criteria) instead.
     *
     * @param hits the hits to group
     * @param criteria the criteria to group on
     * @param maxResultsToStorePerGroup how many results to store per group at most
     */
    protected HitGroupsImpl(Hits hits, HitProperty criteria, int maxResultsToStorePerGroup) {
        super(hits.queryInfo(), criteria);
        
        List<Annotation> requiredContext = criteria.needsContext();
        criteria = criteria.copyWith(hits, requiredContext == null ? null : new Contexts(hits, requiredContext, criteria.needsContextSize(hits.index())));
        
        //Thread currentThread = Thread.currentThread();
        Map<PropertyValue, List<Hit>> groupLists = new HashMap<>();
        Map<PropertyValue, Integer> groupSizes = new HashMap<>();
        for (Hit hit: hits) {
            PropertyValue identity = criteria.get(hit);
            List<Hit> group = groupLists.get(identity);
            if (group == null) {
                group = new ArrayList<>();
                groupLists.put(identity, group);
            }
            if (maxResultsToStorePerGroup < 0 || group.size() < maxResultsToStorePerGroup)
                group.add(hit);
            Integer groupSize = groupSizes.get(identity);
            if (groupSize == null)
                groupSize = 1;
            else
                groupSize++;
            if (groupSize > largestGroupSize)
                largestGroupSize = groupSize;
            groupSizes.put(identity, groupSize);
            totalHits++;
        }
        for (Map.Entry<PropertyValue, List<Hit>> e : groupLists.entrySet()) {
            PropertyValue groupId = e.getKey();
            List<Hit> hitList = e.getValue();
            Integer groupSize = groupSizes.get(groupId);
            HitGroup group = HitGroup.fromList(queryInfo(), groupId, hitList, groupSize);
            groups.put(groupId, group);
            results.add(group);
        }
    }

    protected HitGroupsImpl(QueryInfo queryInfo, List<HitGroup> groups, HitProperty groupCriteria, SampleParameters sampleParameters, WindowStats windowStats) {
        super(queryInfo, groupCriteria);
        this.windowStats = windowStats;
        this.sampleParameters = sampleParameters;
        for (HitGroup group: groups) {
            if (group.size() > largestGroupSize)
                largestGroupSize = group.size();
            totalHits += group.size();
            results.add(group);
            this.groups.put(group.identity(), group);
        }
    }

    /**
     * Get the total number of hits
     *
     * @return the number of hits
     */
    @Override
    public int sumOfGroupSizes() {
        return totalHits;
    }

    /**
     * Return the size of the largest group
     *
     * @return size of the largest group
     */
    @Override
    public int largestGroupSize() {
        return largestGroupSize;
    }

    @Override
    public String toString() {
        return "ResultsGrouper with " + size() + " groups";
    }

    @Override
    public HitGroup get(PropertyValue identity) {
        return groups.get(identity);
    }

    @Override
    public int size() {
        return groups.size();
    }

    @Override
    public WindowStats windowStats() {
        return windowStats;
    }
    
    @Override
    public SampleParameters sampleParameters() {
        return sampleParameters;
    }
    
    @Override
    public HitGroups window(int first, int number) {
        List<HitGroup> resultsWindow = Results.doWindow(this, first, number);
        boolean hasNext = resultsProcessedAtLeast(first + resultsWindow.size() + 1);
        WindowStats windowStats = new WindowStats(hasNext, first, number, resultsWindow.size());
        return HitGroups.fromList(queryInfo(), resultsWindow, criteria, (SampleParameters)null, windowStats);
    }

    @Override
    public HitGroups filteredBy(ResultProperty<HitGroup> property, PropertyValue value) {
        List<HitGroup> list = Results.doFilter(this, property, value);
        return HitGroups.fromList(queryInfo(), list, groupCriteria(), (SampleParameters)null, (WindowStats)null);
    }

    @Override
    public ResultGroups<HitGroup> groupedBy(ResultProperty<HitGroup> criteria, int maxResultsToStorePerGroup) {
        throw new UnsupportedOperationException("Cannot group HitGroups");
    }

    @Override
    public HitGroups withFewerStoredResults(int maximumNumberOfResultsPerGroup) {
        if (maximumNumberOfResultsPerGroup < 0)
            maximumNumberOfResultsPerGroup = Integer.MAX_VALUE;
        List<HitGroup> truncatedGroups = new ArrayList<HitGroup>();
        for (HitGroup group: results) {
            HitGroup newGroup = HitGroup.fromHits(group.identity(), group.storedResults().window(0, maximumNumberOfResultsPerGroup), group.size());
            truncatedGroups.add(newGroup);
        }
        return HitGroups.fromList(queryInfo(), truncatedGroups, criteria, (SampleParameters)null, windowStats);
    }

}