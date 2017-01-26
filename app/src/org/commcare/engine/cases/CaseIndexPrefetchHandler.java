package org.commcare.engine.cases;

import org.commcare.cases.model.Case;
import org.commcare.cases.query.IndexedValueLookup;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.query.QueryCacheEntry;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QueryHandler;
import org.commcare.cases.query.QueryPlanner;
import org.commcare.cases.util.QueryUtils;
import org.commcare.models.database.user.models.CaseIndexTable;
import org.javarosa.core.model.trace.EvaluationTrace;

import java.util.HashMap;
import java.util.Vector;

/**
 * This handler detects contexts in which the query is likely to trip many index lookups and will
 * strategically perform a cache of relevant indexes
 *
 * Created by ctsims on 1/25/2017.
 */

public class CaseIndexPrefetchHandler implements QueryHandler<IndexedValueLookup> {

    private final CaseIndexTable mCaseIndexTable;

    public static final class Cache implements QueryCacheEntry {
        Vector<String> currentlyFetchedIndexKeys = new Vector<>();
        private HashMap<String, Vector<Integer>> indexCache = new HashMap<>();
    }

    public CaseIndexPrefetchHandler(CaseIndexTable caseIndexTable) {
        this.mCaseIndexTable = caseIndexTable;
    }

    @Override
    public int getExpectedRuntime() {
        return 1;
    }

    @Override
    public IndexedValueLookup profileHandledQuerySet(Vector<PredicateProfile> profiles) {
        IndexedValueLookup ret = QueryUtils.getFirstKeyIndexedValue(profiles);
        if(ret != null){
            if (ret.getKey().startsWith(Case.INDEX_CASE_INDEX_PRE)) {
                return ret;
            }
        }
        return null;

    }

    @Override
    public Vector<Integer> loadProfileMatches(IndexedValueLookup querySet, QueryContext context) {
        String indexName = querySet.getKey().substring(Case.INDEX_CASE_INDEX_PRE.length());
        String value = (String)querySet.value;

        Cache cache = context.getQueryCache(Cache.class);
        if(!cache.currentlyFetchedIndexKeys.contains(indexName)) {
            EvaluationTrace trace = new EvaluationTrace("Index Bulk Prefetch [" + indexName + "]");
            mCaseIndexTable.loadIntoIndexTable(cache.indexCache, indexName);
            trace.setOutcome("");
            context.reportTrace(trace);
            cache.currentlyFetchedIndexKeys.add(indexName);
        }
        String cacheKey = indexName + "|" + value;
        return cache.indexCache.get(cacheKey);
    }

    @Override
    public void updateProfiles(IndexedValueLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.remove(querySet);
    }
}
