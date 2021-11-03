package m.co.rh.id.a_news_provider.app.ui.component.settings;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.AsyncSubject;
import m.co.rh.id.a_news_provider.R;
import m.co.rh.id.a_news_provider.app.rx.RxDisposer;
import m.co.rh.id.a_news_provider.app.util.UiUtils;
import m.co.rh.id.a_news_provider.base.BaseApplication;
import m.co.rh.id.a_news_provider.base.provider.FileProvider;
import m.co.rh.id.alogger.ILogger;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.aprovider.Provider;

public class LogPage extends StatefulView<Activity> {
    private static final String TAG = LogPage.class.getName();

    private File mLogFile;
    private transient RxDisposer mRxDisposer;

    @Override
    protected void initState(Activity activity) {
        super.initState(activity);
        mLogFile = BaseApplication.of(activity)
                .getProvider()
                .get(FileProvider.class)
                .getLogFile();
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_log,
                container, false);
        ProgressBar progressBar = view.findViewById(R.id.progress_circular);
        View noRecord = view.findViewById(R.id.no_record);
        ScrollView scrollView = view.findViewById(R.id.scroll_view);
        TextView textView = view.findViewById(R.id.text_content);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        Provider provider = BaseApplication.of(activity).getProvider();
        prepareDisposer(provider);
        fab.setOnClickListener(v -> {
            try {
                UiUtils.shareFile(activity, mLogFile, activity.getString(R.string.share_log_file));
            } catch (Throwable e) {
                provider.get(ILogger.class)
                        .e(TAG, activity.getString(R.string.error_sharing_log_file), e);
            }
        });
        AsyncSubject<File> asyncSubject = AsyncSubject.create();
        asyncSubject.onNext(mLogFile);
        asyncSubject.onComplete();
        mRxDisposer.add("readLogFile",
                asyncSubject.
                        observeOn(Schedulers.from(BaseApplication.of(activity)
                                .getProvider().get(ExecutorService.class)))
                        .map(file -> {
                            if (!file.exists()) {
                                return "";
                            } else {
                                StringBuilder stringBuilder = new StringBuilder();
                                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                                char[] buff = new char[2048];
                                int b = bufferedReader.read(buff);
                                while (b != -1) {
                                    stringBuilder.append(buff);
                                    b = bufferedReader.read(buff);
                                }
                                return stringBuilder.toString();
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {
                            progressBar.setVisibility(View.GONE);
                            textView.setText(s);
                            if (s.isEmpty()) {
                                noRecord.setVisibility(View.VISIBLE);
                                scrollView.setVisibility(View.GONE);
                                fab.setVisibility(View.GONE);
                            } else {
                                noRecord.setVisibility(View.GONE);
                                scrollView.setVisibility(View.VISIBLE);
                                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                                fab.setVisibility(View.VISIBLE);
                            }
                        }));

        return view;
    }

    private void prepareDisposer(Provider provider) {
        if (mRxDisposer != null) {
            mRxDisposer.dispose();
        }
        mRxDisposer = provider.get(RxDisposer.class);
    }
}
