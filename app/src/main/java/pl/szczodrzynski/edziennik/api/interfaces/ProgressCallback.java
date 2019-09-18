package pl.szczodrzynski.edziennik.api.interfaces;

import androidx.annotation.StringRes;

public interface ProgressCallback extends ErrorCallback {
    void onProgress(int progressStep);
    void onActionStarted(@StringRes int stringResId);
}
