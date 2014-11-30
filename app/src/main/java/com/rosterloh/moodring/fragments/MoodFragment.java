package com.rosterloh.moodring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rosterloh.moodring.ui.BaseCab;
import com.rosterloh.moodring.ui.DrawerActivity;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 11/30/2014.
 */
public class MoodFragment extends Fragment implements DrawerActivity.FabListener {

    public MoodFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        DrawerActivity act = (DrawerActivity) getActivity();
        act.toggleFab(false);
        if (!((DrawerLayout) act.findViewById(R.id.drawer_layout)).isDrawerOpen(Gravity.START)) {
            //act.setTitle(mDirectory.getDisplay());
        }

        BaseCab cab = ((DrawerActivity) getActivity()).getCab();
        if (cab != null && cab instanceof BaseFileCab) {
            mAdapter.restoreCheckedPaths(((BaseFileCab) cab).getFiles());
            if (act.shouldAttachFab) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DrawerActivity act = (DrawerActivity) getActivity();
                                BaseFileCab cab = (BaseFileCab) act.getCab()
                                        .setFragment(DirectoryFragment.this);
                                cab.start();
                                act.shouldAttachFab = false;
                            }
                        });
                    }
                }).start();
            } else cab.setFragment(this);
        }

        ((NavigationDrawerFragment) act.getFragmentManager().findFragmentByTag("NAV_DRAWER")).selectFile(mDirectory);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_menu, menu);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview, null);
    }

    @Override
    public void onFabPressed(BaseFileCab.PasteMode pasteMode) {
        if (getActivity() != null) {
            if (pasteMode == BaseFileCab.PasteMode.ENABLED) {
                ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).paste();
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true, new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (dy < 0) {
                    if (dy < -5) {
                        ((DrawerActivity) getActivity()).toggleFab(false);
                    }
                } else if (dy > 0) {
                    if (dy > 10) {
                        ((DrawerActivity) getActivity()).toggleFab(true);
                    }
                }
            }
        }));

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                Utils.getGridMode(getActivity()) ? getResources().getInteger(R.integer.grid_columns) : 1));
        mAdapter = new FileAdapter(getActivity(), this, this, this, mQuery != null);
        mRecyclerView.setAdapter(mAdapter);

        ((DrawerActivity) getActivity()).setFabListener(this);
        reload();
    }
    protected void runOnUiThread(Runnable runnable) {
        Activity act = getActivity();
        if (act != null) act.runOnUiThread(runnable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.goUp:
                ((DrawerActivity) getActivity()).switchDirectory(mDirectory.getParent(), false);
                break;
            case R.id.gridMode:
                boolean gridMode = Utils.getGridMode(getActivity());
                Utils.setGridMode(this, !gridMode);
                break;
            case R.id.sortNameFoldersTop:
                item.setChecked(true);
                Utils.setSorter(this, 0);
                break;
            case R.id.settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIconClicked(int index, File file, boolean added) {
        BaseCab cab = ((DrawerActivity) getActivity()).getCab();
        if (cab != null && (cab instanceof CopyCab || cab instanceof CutCab) && cab.isActive()) {
            if (added) ((BaseFileCab) cab).addFile(file);
            else ((BaseFileCab) cab).removeFile(file);
        } else {
            boolean shouldCreateCab = cab == null || !cab.isActive() || !(cab instanceof MainCab) && added;
            if (shouldCreateCab)
                ((DrawerActivity) getActivity()).setCab(new MainCab()
                        .setFragment(this).setFile(file).start());
            else {
                if (added) ((BaseFileCab) cab).addFile(file);
                else ((BaseFileCab) cab).removeFile(file);
            }
        }
    }

    @Override
    public void onItemClicked(int index, File file) {
        if (file.isDirectory()) {
            ((DrawerActivity) getActivity()).switchDirectory(file, false);
        } else {
            if (((DrawerActivity) getActivity()).pickMode) {
                if (file.isRemote()) {
                    Utils.downloadFile((DrawerActivity) getActivity(), file, new Utils.FileCallback() {
                        @Override
                        public void onFile(File file) {
                            Activity act = getActivity();
                            Intent intent = act.getIntent()
                                    .setData(Uri.fromFile(file.toJavaFile()));
                            act.setResult(Activity.RESULT_OK, intent);
                            act.finish();
                        }
                    });
                } else {
                    Activity act = getActivity();
                    Intent intent = act.getIntent()
                            .setData(Uri.fromFile(file.toJavaFile()));
                    act.setResult(Activity.RESULT_OK, intent);
                    act.finish();
                }
            } else {
                if (file.getExtension().equals("zip")) {
                    final File fFile = file;
                    new MaterialDialog.Builder(getActivity())
                            .positiveColorRes(R.color.cabinet_accent_color)
                            .theme(ThemeUtils.getDialogTheme(getActivity()))
                            .title(R.string.unzip)
                            .content(R.string.auto_unzip_prompt)
                            .positiveText(android.R.string.ok)
                            .negativeText(android.R.string.cancel)
                            .callback(new MaterialDialog.Callback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    List<File> files = new ArrayList<File>();
                                    files.add(fFile);
                                    Unzipper.unzip(DirectoryFragment.this, files, null);
                                }

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    Utils.openFile((DrawerActivity) getActivity(), fFile, false);
                                }
                            })
                            .build().show();
                } else {
                    Utils.openFile((DrawerActivity) getActivity(), file, false);
                }
            }
        }
    }

    @Override
    public void onMenuItemClick(final File file, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pin:
                Pins.add(getActivity(), new Pins.Item(file));
                ((DrawerActivity) getActivity()).reloadNavDrawer(true);
                break;
            case R.id.openAs:
                Utils.openFile((DrawerActivity) getActivity(), file, true);
                break;
            case R.id.copy: {
                BaseCab cab = ((DrawerActivity) getActivity()).getCab();
                boolean shouldCreateCopy = cab == null || !cab.isActive() || !(cab instanceof CopyCab);
                if (shouldCreateCopy) {
                    if (cab != null && cab instanceof BaseFileCab) {
                        ((BaseFileCab) cab).overrideDestroy = true;
                    }
                    ((DrawerActivity) getActivity()).setCab(new CopyCab()
                            .setFragment(this).setFile(file).start());
                } else ((BaseFileCab) cab).setFragment(this).addFile(file);
                break;
            }
            case R.id.cut: {
                BaseCab cab = ((DrawerActivity) getActivity()).getCab();
                boolean shouldCreateCut = cab == null || !cab.isActive() || !(cab instanceof CutCab);
                if (shouldCreateCut) {
                    if (cab != null && cab instanceof BaseFileCab) {
                        ((BaseFileCab) cab).overrideDestroy = true;
                    }
                    ((DrawerActivity) getActivity()).setCab(new CutCab()
                            .setFragment(this).setFile(file).start());
                } else ((BaseFileCab) cab).setFragment(this).addFile(file);
                break;
            }
            case R.id.rename:
                Utils.showInputDialog(getActivity(), R.string.rename, 0, file.getName(), new Utils.InputCallback() {
                    @Override
                    public void onInput(String text) {
                        if (!text.contains("."))
                            text += file.getExtension();
                        final File newFile = file.isRemote() ?
                                new CloudFile(getActivity(), (CloudFile) file.getParent(), text, file.isDirectory()) :
                                new LocalFile(getActivity(), file.getParent(), text);
                        file.rename(newFile, new SftpClient.FileCallback() {
                            @Override
                            public void onComplete(File newFile) {
                                reload();
                                if (((DrawerActivity) getActivity()).getCab() != null &&
                                        ((DrawerActivity) getActivity()).getCab() instanceof BaseFileCab) {
                                    int cabIndex = ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).findFile(file);
                                    if (cabIndex > -1)
                                        ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).setFile(cabIndex, newFile);
                                    Toast.makeText(getActivity(), getString(R.string.renamed_to, newFile.getPath()), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                // Ignore
                            }
                        });
                    }
                });
                break;
            case R.id.zip:
                final List<File> files = new ArrayList<File>();
                files.add(file);
                if (file.getExtension().equals("zip")) {
                    Unzipper.unzip(this, files, null);
                } else {
                    Zipper.zip(this, files, null);
                }
                break;
            case R.id.share:
                if (file.isRemote()) {
                    Utils.downloadFile((DrawerActivity) getActivity(), file, new Utils.FileCallback() {
                        @Override
                        public void onFile(File file) {
                            shareFile(file);
                        }
                    });
                } else {
                    shareFile(file);
                }
                break;
            case R.id.delete:
                Utils.showConfirmDialog(getActivity(), R.string.delete, R.string.confirm_delete, file.getName(), new Utils.ClickListener() {
                    @Override
                    public void onPositive(int which, View view) {
                        file.delete(new SftpClient.CompletionCallback() {
                            @Override
                            public void onComplete() {
                                if (Pins.remove(getActivity(), file))
                                    ((DrawerActivity) getActivity()).reloadNavDrawer();
                                mAdapter.remove(file, true);
                                DrawerActivity act = (DrawerActivity) getActivity();
                                if (act.getCab() != null && act.getCab() instanceof BaseFileCab) {
                                    BaseFileCab cab = (BaseFileCab) act.getCab();
                                    if (cab.getFiles().size() > 0) {
                                        List<File> files = new ArrayList<File>();
                                        files.addAll(cab.getFiles()); // copy so it doesn't get modified by CAB functions
                                        cab.removeFile(file);
                                        for (File fi : files) {
                                            if (fi.getPath().startsWith(file.getPath())) {
                                                cab.removeFile(fi);
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                // Ignore
                            }
                        });
                    }
                });
                break;
            case R.id.details:
                DetailsDialog.create(file).show(getActivity().getFragmentManager(), "DETAILS_DIALOG");
                break;
        }
    }
}
