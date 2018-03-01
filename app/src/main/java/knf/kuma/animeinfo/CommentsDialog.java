package knf.kuma.animeinfo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import knf.kuma.R;
import knf.kuma.pojos.AnimeObject;

public class CommentsDialog {
    Spinner spinner;
    ProgressBar progressBar;
    ScrollView scrollView;
    WebView webView;
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();

    public CommentsDialog(List<AnimeObject.WebInfo.AnimeChapter> chapters) {
        this.chapters = chapters;
    }

    public void show(Activity activity) {
        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .customView(R.layout.layout_comments_dialog, false)
                .cancelable(true)
                .build();
        View view = dialog.getCustomView();
        spinner = view.findViewById(R.id.spinner);
        progressBar = view.findViewById(R.id.progress);
        scrollView = view.findViewById(R.id.scroll);
        webView = view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        String newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
        webView.getSettings().setUserAgentString(newUA);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                onHide();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                onShow();
            }
        });
        spinner.setAdapter(new ArrayAdapter<String>(activity, R.layout.item_simple_spinner, getEps()));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                webView.loadUrl(getLink(chapters.get(position).link));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        webView.loadUrl(getLink(chapters.get(0).link));
        dialog.show();
    }

    private String getLink(String link) {
        try {
            return "https://web.facebook.com/plugins/comments.php?api_key=156149244424100&channel_url=https%3A%2F%2Fstaticxx.facebook.com%2Fconnect%2Fxd_arbiter%2Fr%2FlY4eZXm_YWu.js%3Fversion%3D42%23cb%3Df3448d0a8b0514c%26domain%3Danimeflv.net%26origin%3Dhttps%253A%252F%252Fanimeflv.net%252Ff304e603e6a096%26relation%3Dparent.parent&href=" +
                    URLEncoder.encode(link, "UTF-8") +
                    "&locale=es_LA&numposts=50&sdk=joey&version=v2.9&width=100%25";
        } catch (Exception e) {
            e.printStackTrace();
            return link;
        }
    }

    private void onHide() {
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    private void onShow() {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private String[] getEps() {
        List<String> eps = new ArrayList<>();
        for (AnimeObject.WebInfo.AnimeChapter chapter : chapters) {
            eps.add(chapter.number);
        }
        return eps.toArray(new String[chapters.size()]);
    }
}
