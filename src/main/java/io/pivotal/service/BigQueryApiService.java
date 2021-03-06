package io.pivotal.service;

import com.google.api.services.bigquery.model.TableRow;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.QueryRequest;
import com.google.api.services.bigquery.model.QueryResponse;
import io.pivotal.CredentialManager;
import io.pivotal.domain.LandmarkNameWithScore;

/**
 * Created by mross on 10/7/16.
 */
public class BigQueryApiService {

    private String query;
    private long totalBytesProcessed;
    private String dataSetName = "gdelt-bq:internetarchivebooks";
    private boolean isCached;

    public long getTotalBytesProcessed() {
        return totalBytesProcessed;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public BigQueryApiService(List<LandmarkNameWithScore> lwsList) {
        String landMarkName = "";
        if (lwsList.size() > 1) {
            for (LandmarkNameWithScore lws : lwsList) {
                if (landMarkName.length() > 0) {
                    landMarkName += '|'; // Building an OR for the regular expression
                }
                landMarkName += lws.getName();
            }
            landMarkName = '(' + landMarkName + ')';
        } else {
            landMarkName = lwsList.get(0).getName();
        }
        setQuery(landMarkName);
    }

    public BigQueryApiService(String landmarkName) {
        setQuery(landmarkName);
    }

    private void setQuery(String landmarkName) {
        this.query = String.format(
                "SELECT * FROM ("
                        + "SELECT BookMeta_Title, BookMeta_Creator, BookMeta_Subjects, LENGTH(BookMeta_Title) title_len"
                        // De-dupe based on the columns in the PARTITION BY clause
                        + ", ROW_NUMBER() OVER (PARTITION BY BookMeta_Title, BookMeta_Creator) AS rn"
                        + " FROM (TABLE_QUERY([" + dataSetName + "], "
                        + " 'REGEXP_EXTRACT(table_id, r\"(\\d{4})\") BETWEEN \"1819\" AND \"2014\"'))"
                        + " WHERE (REGEXP_MATCH(LOWER(CONCAT(BookMeta_Title, ' ', BookMeta_Subjects)), r'\\b%s\\b'))"
                        + ") b" +
                        " WHERE b.rn = 1"
                        + " ORDER BY title_len ASC"
                        + " LIMIT 20"
                , landmarkName.toLowerCase().replace("'", "\\'").replaceAll(" +", "\\\\s+"));
        System.out.println("QUERY: \"" + query + "\"");
    }

    public List<TableRow> executeQuery ()  {

        List<TableRow> rows = new ArrayList<TableRow>();

        try {
            CredentialManager credentialManager = new CredentialManager();

            Bigquery bigquery = credentialManager.getBiqQueryClient();
            String projectId = credentialManager.getProjectId();

             rows =
                    executeQuery(
                            query,
                            bigquery,
                            projectId);

            System.out.println("Successfully executed a query");
        } catch (Exception e) {

            System.out.println(e);

        }

        return rows;
    }

    public boolean isCached() {
        return isCached;
    }

    private List<TableRow> executeQuery(String querySql, Bigquery bigquery, String projectId) {
        List<TableRow> rows = new ArrayList<TableRow>();

        try {
            QueryResponse query =
                    bigquery.jobs().query(projectId, new QueryRequest().setQuery(querySql)).execute();

            // Execute it
            GetQueryResultsResponse queryResult =
                    bigquery
                            .jobs()
                            .getQueryResults(
                                    query.getJobReference().getProjectId(), query.getJobReference().getJobId())
                            .execute();
            // TODO: return some metadata about the query (bytes processed, elapsed time, data set)
            totalBytesProcessed = queryResult.getTotalBytesProcessed();
            isCached = queryResult.getCacheHit();
            System.out.println("Total Bytes: " + totalBytesProcessed + (isCached() ? " (cached)" : ""));
            return queryResult.getRows();

        } catch (Exception e) {

            System.out.println(e);
            return rows;
        }
    }


}
