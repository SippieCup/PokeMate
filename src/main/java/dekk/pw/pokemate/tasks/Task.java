package dekk.pw.pokemate.tasks;

import dekk.pw.pokemate.Context;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Created by TimD on 7/21/2016.
 */
public abstract class Task implements Runnable {

    protected static long APIStartTime;
    protected static long APIElapsedTime;

    protected final Context context;

    Task(final Context context) {
        this.context = context;
    }

}
