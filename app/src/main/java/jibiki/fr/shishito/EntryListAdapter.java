package jibiki.fr.shishito;


import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import jibiki.fr.shishito.Models.GramBlock;
import jibiki.fr.shishito.Models.ListEntry;
import jibiki.fr.shishito.Util.ViewUtil;

public class EntryListAdapter extends ArrayAdapter<ListEntry> {
    private final Context context;
    private final ArrayList<ListEntry> values;

    private static final String TAG = EntryListAdapter.class.getSimpleName();

    public EntryListAdapter(Context context, ArrayList<ListEntry> values) {
        super(context, R.layout.word_list_element, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;
        ListEntry entry = values.get(position);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.word_list_element, parent, false);
        }
        TextView vedette = (TextView) rowView.findViewById(R.id.vedette);
        ViewUtil.addVedette(vedette, entry);

        if (rowView.findViewById(0) == null) {
            ViewUtil.addVerified(rowView, entry);
            ArrayList<GramBlock> gramBlocks = entry.getGramBlocks();
            ViewUtil.addGramBlocksToView(rowView, gramBlocks, context);
        }
        //romanji.setText(values.get(position).getRomaji());

        if (position % 2 == 1) {
            rowView.setBackgroundColor(rowView.getResources().getColor(R.color.green));
        } else {
            rowView.setBackgroundColor(rowView.getResources().getColor(R.color.light_green));
        }

        return rowView;
    }
}
