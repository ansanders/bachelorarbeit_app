package development.andre.sanders.bachelorprojectapp.model.matching;

import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;

/**
 * Created by andre on 10.07.17.
 * <p>
 * Factory für die Matcher
 */

public class MatcherFactory {



    private CalculationListener calculationListener;

    public MatcherFactory(CalculationListener listener1) {


        this.calculationListener = listener1;
    }

    public Matcher getFilter(String filterType) {
        if (filterType == null) {
            return null;
        }

        if (filterType.equalsIgnoreCase("student")) {
            return new StudentMatcher(calculationListener);
        } else if (filterType.equalsIgnoreCase("museum")) {
            return new StudentMatcher(calculationListener);
        }

        return null;
    }
}
