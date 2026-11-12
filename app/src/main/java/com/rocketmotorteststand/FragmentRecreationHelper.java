package com.rocketmotorteststand;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * @description: Prevents crashes when Android recreates a tabbed screen (after the process was
 * killed in the background, or after a language change).
 * The tab fragments of this application receive their data through constructor parameters.
 * When Android recreates them itself it uses the empty constructor, so those fragments have no
 * data and crashed. Each tabbed activity calls discardRestoredFragments() right after
 * super.onCreate(): the fragments restored by the system are removed and the activity creates
 * fresh, fully initialised ones exactly as on a first launch.
 * @author: boris.dureau@neuf.fr
 **/
public final class FragmentRecreationHelper {

    private FragmentRecreationHelper() {
        // Utility class: no instances
    }

    /**
     * Removes every fragment restored by the system from the activity's fragment manager.
     * Must be called in onCreate(), right after super.onCreate(savedInstanceState).
     */
    public static void discardRestoredFragments(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.getFragments().isEmpty())
            return;
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fm.getFragments()) {
            transaction.remove(fragment);
        }
        transaction.commitNow();
    }
}
