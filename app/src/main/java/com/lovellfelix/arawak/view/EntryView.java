package com.lovellfelix.arawak.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.lovellfelix.arawak.Constants;
import com.lovellfelix.arawak.R;
import com.lovellfelix.arawak.utils.HtmlUtils;
import com.lovellfelix.arawak.utils.PrefUtils;

import java.util.Date;

public class EntryView extends WebView {

    public interface OnActionListener {
        public void onClickOriginalText();

        public void onClickFullText();

        public void onClickEnclosure();
    }

    private static final String STATE_SCROLL_PERCENTAGE = "STATE_SCROLL_PERCENTAGE";

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";

    private static final String BACKGROUND_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#f6f6f6" : "#181b1f";
    private static final String TEXT_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#000000" : "#C0C0C0";
    private static final String BUTTON_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#ddecf1" : "#475773";

    private static final String CSS = "<head><style type='text/css'> "
            + "body {max-width: 100%; margin: 1.8em 0.3cm 0.3cm 0.2cm; font-family: sans-serif-light; color: " + TEXT_COLOR + "; background-color:" + BACKGROUND_COLOR + "; line-height: 140%} "
            + "* {max-width: 100%; word-break: break-word}"
            + "h1, h2 {font-weight: normal; line-height: 130%} "
            + "h1 {font-size: 170%} "
            + "h2 {font-size: 140%} "
            + "a {color: #97ACE5}"
            + "h1 a {color: inherit; text-decoration: none}"
            + "img {height: auto} "
            + "pre {white-space: pre-wrap;} "
            + "blockquote {margin: 0.8em 0 0.8em 1.2em; padding: 0} "
            + "p {margin: 0.8em 0 0.8em 0} "
            + "p.subtitle {font-size: 80%; border-bottom: 1px solid #33b5e5; margin-bottom: 1.5em} "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
            + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
            + ".button-section p {margin: 0.1cm 0 0.2cm 0}"
            + ".button-section p.marginfix {margin: 0.5cm 0 0.5cm 0}"
            + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; background-color: " + BUTTON_COLOR + "; color: " + TEXT_COLOR + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
            + "</style><meta name='viewport' content='width=device-width'/></head>";

    private static final String BODY_START = "<body>";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START = "<h1><a href='";
    private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static final String BUTTON_START = "<p><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "'/></p>";
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private float mScrollPercentage = 0;

    private OnActionListener mListener;

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();

    public EntryView(Context context) {
        super(context);

        setupWebview();
    }

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupWebview();
    }

    public EntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setupWebview();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superInstanceState", super.onSaveInstanceState());
        bundle.putFloat(STATE_SCROLL_PERCENTAGE, getScrollPercentage());

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mScrollPercentage = bundle.getFloat(STATE_SCROLL_PERCENTAGE);
        super.onRestoreInstanceState(bundle.getParcelable("superInstanceState"));
    }

    public float getScrollPercentage() {
        float positionTopView = getTop();
        float contentHeight = getContentHeight();
        float currentScrollPosition = getScrollY();

        return (currentScrollPosition - positionTopView) / contentHeight;
    }

    public void setScrollPercentage(float scrollPercentage) {
        mScrollPercentage = scrollPercentage;
    }

    public void setListener(OnActionListener listener) {
        mListener = listener;
    }

    public void setHtml(long entryId, String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        if (PrefUtils.getBoolean(PrefUtils.DISABLE_PICTURES, false)) {
            contentText = contentText.replaceAll(HTML_IMG_REGEX, "");
            getSettings().setBlockNetworkImage(true);
        } else {
            if (PrefUtils.getBoolean(PrefUtils.FETCH_PICTURES, false)) {
                contentText = HtmlUtils.replaceImageURLs(contentText, entryId);
            }

            if (getSettings().getBlockNetworkImage()) {
                // setBlockNetwortImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
                loadData("", TEXT_HTML, Constants.UTF8);
                getSettings().setBlockNetworkImage(false);
            }
        }

        // String baseUrl = "";
        // try {
        // URL url = new URL(mLink);
        // baseUrl = url.getProtocol() + "://" + url.getHost();
        // } catch (MalformedURLException ignored) {
        // }

        // do not put 'null' to the base url...
        loadDataWithBaseURL("", generateHtmlContent(title, link, contentText, enclosure, author, timestamp, preferFullText), TEXT_HTML, Constants.UTF8, null);
    }

    private String generateHtmlContent(String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        StringBuilder content = new StringBuilder(CSS).append(BODY_START);

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }

        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText).append(BUTTON_SECTION_START).append(BUTTON_START);

        if (!preferFullText) {
            content.append(context.getString(R.string.get_full_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickFullText();");
        } else {
            content.append(context.getString(R.string.original_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickOriginalText();");
        }
        content.append(BUTTON_END);

        if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
            content.append(BUTTON_START).append(context.getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END);
        }

        if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
        }

        content.append(BUTTON_SECTION_END).append(BODY_END);

        return content.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebview() {
        // For scrolling
        setHorizontalScrollBarEnabled(false);
        getSettings().setUseWideViewPort(false);

        // For color
        setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));

        // Text zoom level from preferences
        int fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "0"));
        if (fontSize != 0) {
            getSettings().setTextZoom(100 + (fontSize * 8));
        }

        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());

        // For HTML5 video
        setWebChromeClient(new WebChromeClient());

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (mScrollPercentage != 0) {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            float webviewSize = getContentHeight() - getTop();
                            float positionInWV = webviewSize * mScrollPercentage;
                            int positionY = Math.round(getTop() + positionInWV);
                            scrollTo(0, positionY);
                        }
                        // Delay the scrollTo to make it work
                    }, 150);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Context context = getContext();
                try {
                    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            mListener.onClickOriginalText();
        }

        @JavascriptInterface
        public void onClickFullText() {
            mListener.onClickFullText();
        }

        @JavascriptInterface
        public void onClickEnclosure() {
            mListener.onClickEnclosure();
        }
    }
}
