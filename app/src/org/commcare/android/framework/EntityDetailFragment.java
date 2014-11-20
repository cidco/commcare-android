package org.commcare.android.framework;

import org.commcare.android.adapters.EntityDetailAdapter;
import org.commcare.android.models.AndroidSessionWrapper;
import org.commcare.android.models.Entity;
import org.commcare.android.models.NodeEntityFactory;
import org.commcare.android.util.DetailCalloutListener;
import org.commcare.android.util.SerializationUtil;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.suite.model.Detail;
import org.javarosa.core.model.instance.TreeReference;
import org.odk.collect.android.views.media.AudioController;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Fragment to display Detail content. Not meant for handling nested Detail objects.
 * @author jschweers
 *
 */
public class EntityDetailFragment extends Fragment {
    public static final String CHILD_DETAIL_INDEX = "edf_child_detail_index";
    public static final String DETAIL_ID = "edf_detail_id";
    public static final String DETAIL_INDEX = "edf_detail_index";
    public static final String HAS_DETAIL_CALLOUT_LISTENER = "edf_has_detail_callout_listener";
    public static final String CHILD_REFERENCE = "edf_detail_reference";
    
    private AndroidSessionWrapper asw;
    private NodeEntityFactory factory;
    private EntityDetailAdapter adapter;

    public EntityDetailFragment() {
        super();
        this.asw = CommCareApplication._().getCurrentSessionWrapper();
    }
    
    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Note that some of this setup could be moved into onAttach if it would help performance
        Bundle args = getArguments();

        Detail detail = asw.getSession().getDetail(args.getString(DETAIL_ID));
        Detail childDetail = detail;
        if (args.getInt(CHILD_DETAIL_INDEX, -1) != -1) {
            childDetail = detail.getDetails()[args.getInt(CHILD_DETAIL_INDEX)];
        }

        factory = new NodeEntityFactory(childDetail, asw.getEvaluationContext());
        Entity entity = factory.getEntity(SerializationUtil.deserializeFromBundle(
            args, CHILD_REFERENCE, TreeReference.class)
        );

        View rootView = inflater.inflate(R.layout.entity_detail_list, container, false);
        Activity thisActivity = getActivity();
        AudioController audioController  = null;
        DetailCalloutListener detailCalloutListener = null;
        if(thisActivity instanceof AudioController) {
            audioController = (AudioController)thisActivity;
        } 
        if(thisActivity instanceof DetailCalloutListener) {
            detailCalloutListener = (DetailCalloutListener)thisActivity;
        } 

        
        adapter = new EntityDetailAdapter(
            thisActivity, asw.getSession(), childDetail, entity, 
            detailCalloutListener, audioController, args.getInt(DETAIL_INDEX)
        );
        ((ListView) rootView.findViewById(R.id.screen_entity_detail_list)).setAdapter(adapter);
        return rootView;
    }

}
