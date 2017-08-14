package development.andre.sanders.bachelorprojectapp.model.manager;

import com.squareup.otto.Bus;

/**
 * Created by andre on 12.08.17.
 */

public class EventBus {

    private static EventBus instance;
    private Bus localBus = new Bus();

    //Instance
    public static EventBus getInstance() {

        if (instance == null)
            instance = new EventBus();

        return instance;

    }

    public Bus getBus(){
        return localBus;
    }
}
