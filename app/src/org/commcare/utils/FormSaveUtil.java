package org.commcare.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.commcare.CommCareApplication;
import org.commcare.provider.FormsProviderAPI;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormSaveUtil {

    /**
     * @param context used to query form instances through android's content provider framework
     * @return A mapping from an installed form's namespace to its install path.
     */
    public static Hashtable<String, String> getNamespaceToFilePathMap(Context context) {
        Hashtable<String, String> formNamespaces = new Hashtable<>();

        for (String xmlns : CommCareApplication.instance().getCommCarePlatform().getInstalledForms()) {
            Cursor cur = null;
            try {
                Uri formContentUri = CommCareApplication.instance().getCommCarePlatform().getFormContentUri(xmlns);
                cur = context.getContentResolver().query(formContentUri, new String[]{FormsProviderAPI.FormsColumns.FORM_FILE_PATH}, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    String path = cur.getString(cur.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_FILE_PATH));
                    formNamespaces.put(xmlns, path);
                } else {
                    throw new RuntimeException("No form registered for xmlns at content URI: " +
                            CommCareApplication.instance().getCommCarePlatform().getFormContentUri(xmlns));
                }
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }
        return formNamespaces;
    }
}
