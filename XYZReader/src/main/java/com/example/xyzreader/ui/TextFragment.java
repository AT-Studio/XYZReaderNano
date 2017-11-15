package com.example.xyzreader.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by AliT on 11/10/17.
 */

public class TextFragment extends Fragment {

    public static final String STRING_EXTA = "text";
    public static final String PAGE_NUMBER_EXTRA = "page";
    public static final String TOT_PAGES_EXTRA = "totPages";

    public static TextFragment create(String text, int page, int totPages) {

        TextFragment fragment = new TextFragment();

        Bundle bundle = new Bundle();
        bundle.putString(STRING_EXTA, text);
        bundle.putInt(PAGE_NUMBER_EXTRA, page);
        bundle.putInt(TOT_PAGES_EXTRA, totPages);

        fragment.setArguments(bundle);

        return fragment;
    }

    @BindView(R.id.textView)
    TextView textView;

    @BindView(R.id.pageNumber)
    TextView pageNumber;

    Unbinder unbinder;

    String text;
    int page;
    int totPages;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        this.text = bundle.getString(STRING_EXTA);
        this.page = bundle.getInt(PAGE_NUMBER_EXTRA);
        this.totPages = bundle.getInt(TOT_PAGES_EXTRA);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.text_fragment_layout, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        textView.setText(text);

        pageNumber.setText(page + " of " + totPages);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
