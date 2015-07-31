package org.commcare.android.tasks;

import android.os.SystemClock;

import org.commcare.android.tasks.templates.ManagedAsyncTask;

/**
 * Upgrades the seated app in the background. If the user opens the Upgrade
 * activity, this task will report its progress to that activity. Enforces the
 * constraint that only one instance is ever running.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class UpgradeTask extends ManagedAsyncTask<String, int[], Boolean> {
    private static final String TAG = UpgradeTask.class.getSimpleName();

    private TaskListener<int[], Boolean> taskListener = null;

    private static UpgradeTask singletonRunningInstance = null;
    private int progress = 0;

    private UpgradeTask() {
    }

    public static UpgradeTask getNewInstance() {
        if (singletonRunningInstance == null) {
            singletonRunningInstance = new UpgradeTask();
            return singletonRunningInstance;
        } else {
            throw new IllegalStateException("There is a " + TAG + " instance.");
        }
    }

    public static UpgradeTask getRunningInstance() {
        if (singletonRunningInstance != null &&
                singletonRunningInstance.getStatus() == Status.RUNNING) {
            return singletonRunningInstance;
        }
        return null;
    }

    @Override
    protected final Boolean doInBackground(String... params) {
        while (progress < 101) {
            if (isCancelled()) {
                SystemClock.sleep(3000);
                return false;
            }

            SystemClock.sleep(500);
            publishProgress(new int[]{progress++, 100});
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(int[]... values) {
        super.onProgressUpdate(values);

        if (taskListener != null) {
            taskListener.processTaskUpdate(values);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (taskListener != null) {
            taskListener.processTaskResult(result);
        }

        singletonRunningInstance = null;
    }

    @Override
    protected void onCancelled(Boolean result) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            super.onCancelled(result);
        } else {
            super.onCancelled();
        }

        if (taskListener != null) {
            taskListener.processTaskCancel(result);
        }

        singletonRunningInstance = null;
    }

    public void registerTaskListener(TaskListener<int[], Boolean> listener)
            throws TaskListenerException {
        if (taskListener != null) {
            throw new TaskListenerException("This " + TAG +
                    " was already registered with a TaskListener");
        }
        taskListener = listener;
    }

    public void unregisterTaskListener(TaskListener<int[], Boolean> listener)
            throws TaskListenerException {
        if (listener != taskListener) {
            throw new TaskListenerException("The provided listener wasn't " +
                    "registered with this " + TAG);
        }
        taskListener = null;
    }

    public int getProgress() {
        return progress;
    }
}
