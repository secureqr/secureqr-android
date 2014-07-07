package at.rieder.secureqr.app.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Created by Thomas on 25.03.14.
 */
public class HistoryItem implements Comparable<HistoryItem>, Serializable {

    private Content content;
    private Collection<CheckResult> checkResults;
    private Date date;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryItem that = (HistoryItem) o;

        if (checkResults != null ? !checkResults.equals(that.checkResults) : that.checkResults != null)
            return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + (checkResults != null ? checkResults.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    public Content getContent() {

        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public Collection<CheckResult> getCheckResult() {
        return checkResults;
    }

    public void setCheckResult(Collection<CheckResult> checkResult) {
        this.checkResults = checkResult;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public int compareTo(HistoryItem historyItem) {
        return this.date.compareTo(historyItem.date) * -1;
    }
}
