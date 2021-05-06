package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.features.media.MediaListFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MediaReviewListFragment extends MediaListFragment {

    protected long mStatus = Media.STATUS_LOCAL;
    protected long[] mStatuses = {Media.STATUS_LOCAL};

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaReviewListFragment() {
    }

    public void setStatus (long status) {
        mStatus = status;
    }

    public void refresh ()
    {
        if (getMMediaAdapter() != null)
        {
            List<Media> listMedia = Media.Companion.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);
            ArrayList listMediaArray = new ArrayList(listMedia);
            getMMediaAdapter().updateData(listMediaArray);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_list_simple, container, false);
        rootView.setTag(TAG);

        LinearLayout mMediaContainer = rootView.findViewById(R.id.mediacontainer);

        RecyclerView rView = new RecyclerView(getContext());
        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.setHasFixedSize(true);

        rView = rootView.findViewById(R.id.recyclerview);
        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.setHasFixedSize(true);

        List<Media> listMedia = Media.Companion.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);

        ArrayList listMediaArray = new ArrayList(listMedia);
        MediaAdapter mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row, listMediaArray, rView, (OnStartDragListener) viewHolder -> {

        });

        setMMediaAdapter(mMediaAdapter);

        mMediaAdapter.setDoImageFade(false);
        rView.setAdapter(mMediaAdapter);

        return rootView;
    }


}