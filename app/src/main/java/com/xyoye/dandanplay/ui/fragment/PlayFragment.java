package com.xyoye.dandanplay.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.base.BaseFragment;
import com.xyoye.dandanplay.base.BaseRvAdapter;
import com.xyoye.dandanplay.bean.FolderBean;
import com.xyoye.dandanplay.bean.VideoBean;
import com.xyoye.dandanplay.bean.event.OpenFolderEvent;
import com.xyoye.dandanplay.bean.event.RefreshFolderEvent;
import com.xyoye.dandanplay.mvp.impl.PlayFragmentPresenterImpl;
import com.xyoye.dandanplay.mvp.presenter.PlayFragmentPresenter;
import com.xyoye.dandanplay.mvp.view.PlayFragmentView;
import com.xyoye.dandanplay.ui.activities.FolderActivity;
import com.xyoye.dandanplay.ui.activities.PlayerActivity;
import com.xyoye.dandanplay.ui.activities.PlayerExoActivity;
import com.xyoye.dandanplay.ui.weight.item.FolderItem;
import com.xyoye.dandanplay.utils.AppConfig;
import com.xyoye.dandanplay.utils.JsonUtil;
import com.xyoye.dandanplay.utils.interf.AdapterItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by YE on 2018/6/29 0029.
 */

public class PlayFragment extends BaseFragment<PlayFragmentPresenter> implements PlayFragmentView {
    private static final int DIRECTORY_CHOOSE_REQ_CODE = 106;

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout refresh;
    @BindView(R.id.rv)
    RecyclerView recyclerView;
    @BindView(R.id.fast_play_bt)
    FloatingActionButton fastPlayBt;

    private BaseRvAdapter<FolderBean> adapter;

    public static PlayFragment newInstance() {
        return new PlayFragment();
    }

    @NonNull
    @Override
    protected PlayFragmentPresenter initPresenter() {
        return new PlayFragmentPresenterImpl(this, this);
    }

    @Override
    protected int initPageLayoutId() {
        return R.layout.fragment_play;
    }

    @SuppressLint("CheckResult")
    @Override
    public void initView() {
        refresh.setColorSchemeResources(R.color.theme_color);

        adapter = new BaseRvAdapter<FolderBean>(new ArrayList<>()) {
            @NonNull
            @Override
            public AdapterItem<FolderBean> onCreateItem(int viewType) {
                return new FolderItem();
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setAdapter(adapter);

        fastPlayBt.setOnClickListener(v -> {
            String videoInfo = AppConfig.getInstance().getLastPlayVideo();
            if (!StringUtils.isEmpty(videoInfo)){
                VideoBean videoBean = JsonUtil.fromJson(videoInfo, VideoBean.class);
                if (videoBean == null)
                    return;
                //视频文件是否已被删除
                File videoFile = new File(videoBean.getVideoPath());
                if (!videoFile.exists())
                    return;
                //弹幕文件是否已被删除
                if (!StringUtils.isEmpty(videoBean.getDanmuPath())){
                    File danmuFile = new File(videoBean.getDanmuPath());
                    if (!danmuFile.exists())
                        videoBean.setDanmuPath("");
                }

                //选择的播放器
                Intent intent;
                if (AppConfig.getInstance().getPlayerType() == com.player.ijkplayer.utils.Constants.IJK_EXO_PLAYER)
                    intent = new Intent(getContext(), PlayerExoActivity.class);
                else
                    intent = new Intent(getContext(), PlayerActivity.class);
                String title = FileUtils.getFileNameNoExtension(videoBean.getVideoPath());
                intent.putExtra("title", title);
                intent.putExtra("path", videoBean.getVideoPath());
                intent.putExtra("danmu_path", videoBean.getDanmuPath());
                intent.putExtra("current", videoBean.getVideoDuration());
                intent.putExtra("episode_id", videoBean.getEpisodeId());
                startActivity(intent);
            }
        });

        refresh.setRefreshing(true);
        refreshVideo(false);
    }

    @Override
    public void initListener() {
        refresh.setOnRefreshListener(() -> refreshVideo(true));
    }

    @Override
    public void refreshAdapter(List<FolderBean> beans) {
        adapter.setData(beans);
        hideLoading();
        if (refresh != null)
            refresh.setRefreshing(false);
    }

    @Override
    public void refreshOver() {
        if (refresh != null)
            refresh.setRefreshing(false);
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showError(String message) {
        ToastUtils.showShort(message);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void openFolder(OpenFolderEvent event) {
        Intent intent = new Intent(getContext(), FolderActivity.class);
        intent.putExtra(OpenFolderEvent.FOLDERPATH, event.getFolderPath());
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshFolderEvent(RefreshFolderEvent event){
        if (event.isReGetData())
            presenter.getVideoFormDatabase();
        else
            adapter.notifyDataSetChanged();
    }

    @SuppressLint("CheckResult")
    private void refreshVideo(boolean isAll){
        new RxPermissions(this).
                request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {
                        //通知系统刷新
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
                        if (getContext() != null)
                            getContext().sendBroadcast(intent);
                        if (isAll)
                            presenter.getVideoFormSystemAndSave();
                        else
                            presenter.getVideoFormSystem();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == DIRECTORY_CHOOSE_REQ_CODE){
                Uri SDCardUri = data.getData();
                if (SDCardUri != null){
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.getContentResolver().takePersistableUriPermission(SDCardUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        AppConfig.getInstance().setSDFolderUri(SDCardUri.toString());
                    }
                }else {
                    ToastUtils.showShort("未获取外置存储卡权限，无法操作外置存储卡");
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void registerEventBus(){
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    public void unregisterEventBus(){
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }
}
