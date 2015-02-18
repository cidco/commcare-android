/**
 * 
 */
package org.commcare.android.database.global.models;

import org.commcare.android.database.SqlStorage;
import org.commcare.android.database.app.DatabaseAppOpenHelper;
import org.commcare.android.database.app.models.UserKeyRecord;
import org.commcare.android.database.user.CommCareUserOpenHelper;
import org.commcare.android.storage.framework.MetaField;
import org.commcare.android.storage.framework.Persisted;
import org.commcare.android.storage.framework.Persisting;
import org.commcare.android.storage.framework.Table;
import org.commcare.dalvik.application.CommCareApp;
import org.commcare.dalvik.application.CommCareApplication;

import android.content.Context;

/**
 * An Application Record tracks an individual CommCare app on the current
 * install.
 * 
 * @author ctsims
 *
 */
@Table("app_record")
public class ApplicationRecord extends Persisted {
    public static final String STORAGE_KEY = "app_record";

    public static final String META_STATUS = "status";
    
    public static final int STATUS_UNINITIALIZED = 0;
    public static final int STATUS_INSTALLED = 1;
    public static final int STATUS_DELETE_REQUESTED = 2;
    /**
     * The app needs to be upgraded from an old version
     */
    public static final int STATUS_SPECIAL_LEGACY = 2;

    @Persisting(1)
    String applicationId;
    @Persisting(2)
    @MetaField(META_STATUS)
    int status;
    @Persisting(3)
    String uniqueId;
    @Persisting(4)
    String displayName;
    @Persisting(5)
    boolean resourcesValidated;
    @Persisting(6)
    boolean isArchived;
    @Persisting(7)
    boolean convertedViaDbUpgrader;
    @Persisting(8)
    boolean preMultipleAppsProfile;
    
    /*
     * Deserialization only
     */
    public ApplicationRecord() {
        
    }
    
    public ApplicationRecord(String applicationId, int status) {
        this.applicationId = applicationId;
        this.status = status;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String id) {
        this.uniqueId = id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String appName) {
        this.displayName = appName;
    }

    public void setArchiveStatus(boolean b) {
        this.isArchived = b;
    }

    public boolean isArchived() {
        return this.isArchived;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    public void setResourcesStatus(boolean b) {
        this.resourcesValidated = b;
    }

    public boolean resourcesValidated() {
        return this.resourcesValidated;
    }

    /*
     * Returns true if this ApplicationRecord represents an app generated from
     * a pre-Multiple Apps version of CommCare that does not have profile files
     * with uniqueId and displayName
     */
    public boolean preMultipleAppsProfile() {
        return this.preMultipleAppsProfile;
    }
    
    public void setPreMultipleAppsProfile(boolean b) {
        this.preMultipleAppsProfile = b;
    }

    /*
     * Returns true if this ApplicationRecord was just generated from the a
     * different ApplicationRecord format via the db upgrader, because it was 
     * initially installed on a phone with a pre-Multiple Apps version of CommCare
     */
    public boolean convertedByDbUpgrader() {
        return this.convertedViaDbUpgrader;
    }

    public void setConvertedByDbUpgrader(boolean b) {
        this.convertedViaDbUpgrader = b;
    }
    
    public void uninstall(Context c) {
        CommCareApplication._().initializeAppResources(new CommCareApp(this));
        CommCareApp app = CommCareApplication._().getCurrentApp();
        
        //1) Set states to delete requested so we know if we have left the app in a bad state
        CommCareApplication._().setAppResourceState(CommCareApplication.STATE_DELETE_REQUESTED);
        this.setStatus(ApplicationRecord.STATUS_DELETE_REQUESTED);
        CommCareApplication._().getGlobalStorage(ApplicationRecord.class).write(this);
        //2) Teardown the sandbox for this app
        app.teardownSandbox();   
        //3) Delete all the user databases associated with this app
        SqlStorage<UserKeyRecord> userDatabase = CommCareApplication._().getAppStorage(UserKeyRecord.class);
        for (UserKeyRecord user : userDatabase) {
            c.getDatabasePath(CommCareUserOpenHelper.getDbName(user.getUuid())).delete();
        }
        //4) Delete the app database
        c.getDatabasePath(DatabaseAppOpenHelper.getDbName(app.getAppRecord().getApplicationId())).delete();
        //5) Delete the app record
        CommCareApplication._().getGlobalStorage(ApplicationRecord.class).remove(this.getID());
        //6) Reset the appResourceState in CCApplication
        CommCareApplication._().setAppResourceState(CommCareApplication.STATE_UNINSTALLED);
    }

}
