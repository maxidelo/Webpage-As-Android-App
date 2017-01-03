package com.maxidelo.webapp;

/**
 * Logs to the console
 */
public class Log {

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    private final String className;

    // ----------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------

    public Log(String className) {
        this.className = className;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * Logs on debug mode only if the application has the debug enabled
     *
     * @param message the message to log
     */
    public void d(String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(className, message);
        }
    }


    /**
     * Logs on error mode
     *
     * @param message the message to log
     */
    public void e(String message) {
        android.util.Log.e(className, message);
    }


}
