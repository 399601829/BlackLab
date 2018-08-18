package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.search.results.HitGroup;
import nl.inl.blacklab.search.results.HitGroups;
import nl.inl.blacklab.search.results.Hits;
import nl.inl.blacklab.search.results.WindowStats;
import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.datastream.DataStream;
import nl.inl.blacklab.server.exceptions.BlsException;
import nl.inl.blacklab.server.jobs.JobHitsGrouped;
import nl.inl.blacklab.server.jobs.User;
import nl.inl.blacklab.server.jobs.WindowSettings;

/**
 * Request handler for grouped hit results.
 */
public class RequestHandlerHitsGrouped extends RequestHandler {

    public RequestHandlerHitsGrouped(BlackLabServer servlet, HttpServletRequest request, User user, String indexName,
            String urlResource, String urlPathPart) {
        super(servlet, request, user, indexName, urlResource, urlPathPart);
    }

    @Override
    public int handle(DataStream ds) throws BlsException {
        // Get the window we're interested in
        JobHitsGrouped search = (JobHitsGrouped) searchMan.search(user, searchParam.hitsGrouped(), true);
        try {
            // If search is not done yet, indicate this to the user
            if (!search.finished()) {
                return Response.busy(ds, servlet);
            }

            // Search is done; construct the results object
            final HitGroups groups = search.getGroups();

            ds.startMap();
            ds.startEntry("summary").startMap();
            Hits hits = search.getHits();
            WindowSettings windowSettings = searchParam.getWindowSettings();
            final int first = windowSettings.first() < 0 ? 0 : windowSettings.first();
            final int requestedWindowSize = windowSettings.size() < 0
                    || windowSettings.size() > searchMan.config().maxPageSize() ? searchMan.config().defaultPageSize()
                            : windowSettings.size();
            int totalResults = groups.size();
            final int actualWindowSize = first + requestedWindowSize > totalResults ? totalResults - first
                    : requestedWindowSize;
            WindowStats ourWindow = new WindowStats(first + requestedWindowSize < totalResults, first, requestedWindowSize, actualWindowSize);
            addSummaryCommonFields(ds, searchParam, search.userWaitTime(), 0, groups, ourWindow);
            addNumberOfResultsSummaryTotalHits(ds, hits, false);
            ds.endMap().endEntry();

            // The list of groups found
            ds.startEntry("hitGroups").startList();
            int i = 0;
            for (HitGroup group : groups) {
                if (i >= first && i < first + requestedWindowSize) {
                    ds.startItem("hitgroup").startMap();
                    ds.entry("identity", group.identity().serialize())
                            .entry("identityDisplay", group.identity().toString())
                            .entry("size", group.size());
                    ds.endMap().endItem();
                }
                i++;
            }
            ds.endList().endEntry();
            ds.endMap();

            return HTTP_OK;
        } finally {
            search.decrRef();
        }
    }

}
