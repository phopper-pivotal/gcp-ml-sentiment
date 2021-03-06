package io.pivotal.domain;

import com.google.api.services.bigquery.model.TableCell;
import com.google.api.services.bigquery.model.TableRow;

/**
 * Created by mross on 10/7/16.
 */
public class QueryResultsViewMapping {

    String bookName;
    String authorName;
    String bookLocation;

    private static final int MAX_STRING_LEN = 48;
    private static final String DEFAULT_VALUE = "(Not available)";

    public QueryResultsViewMapping() {

        this.bookName = "N/A";
        this.authorName = "N/A";
        this.bookLocation = "N/A";
    }

    public QueryResultsViewMapping(TableRow rows) {
        int count = 0;

        for (TableCell cell : rows.getF()) {
            // Why are we seeing "author = java.lang.Object@3d347088"?
            Object cellObj = cell.getV();
            String stringVal = DEFAULT_VALUE;
            if (cellObj != null) {
                stringVal = cell.getV().toString();
                if (stringVal.length() > MAX_STRING_LEN) {
                  stringVal = stringVal.substring(0, MAX_STRING_LEN) + " ...";
                }
                if (stringVal.startsWith("java.lang.Object@")) {
                    stringVal = DEFAULT_VALUE;
                }
            }
            if (count == 0) {
                bookName = stringVal;
            } else if (count == 1) {
                authorName = stringVal;
            } else if (count == 2) {
                bookLocation = stringVal;
            }
            count++;
        }

    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getBookLocation() {
        return bookLocation;
    }

    public void setBookLocation(String bookLocation) {
        this.bookLocation = bookLocation;
    }

    @Override
    public String toString() {
        return "QueryResultsViewMapping{" +
                "bookName='" + bookName + '\'' +
                ", authorName='" + authorName + '\'' +
                ", bookLocation='" + bookLocation + '\'' +
                '}';
    }
}
