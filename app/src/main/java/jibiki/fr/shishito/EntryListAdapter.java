package jibiki.fr.shishito;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import jibiki.fr.shishito.Models.ListEntry;
import jibiki.fr.shishito.Util.ViewUtil;

class EntryListAdapter extends ArrayAdapter<ListEntry> {
    private final Context context;
    private transient final ArrayList<ListEntry> values;

    @SuppressWarnings("unused")
    private static final String TAG = EntryListAdapter.class.getSimpleName();

    public EntryListAdapter(Context context, ArrayList<ListEntry> values) {
        super(context, R.layout.word_list_element, values);
        this.context = context;
        this.values = values;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;
        ListEntry entry = values.get(position);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.word_list_element, parent, false);

            TextView vedette = (TextView) rowView.findViewById(R.id.vedette);
            ViewUtil.addVedette(vedette, entry, context, false);
            ViewUtil.addVerified(rowView, entry);
            ViewUtil.parseAndAddGramBlocksToView(rowView, entry, context, false);
        }
        if (position % 2 == 1) {
            rowView.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
        } else {
            rowView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green));
        }

        return rowView;
    }
}
