package development.andre.sanders.bachelorprojectapp.model.callbacks;

import development.andre.sanders.bachelorprojectapp.model.callbacks.events.MatchingResultEvent;

/**
 * Created by andre on 14.08.17.
 */

public interface MatchingResultListener {
    void onMatchingResult(MatchingResultEvent resultEvent);
    void addResult(Boolean result);
}
