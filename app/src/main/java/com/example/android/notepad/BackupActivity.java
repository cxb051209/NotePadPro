package com.example.android.notepad;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 云备份与恢复页。
 *
 * <ul>
 *   <li>立即备份：把全部笔记打包成 JSON，写入云端备份空间</li>
 *   <li>备份记录：点击可覆盖恢复或删除</li>
 * </ul>
 */
public class BackupActivity extends AppCompatActivity implements BackupAdapter.Listener {

    private NoteRepository repository;
    private BackupAdapter adapter;
    private TextView textEmpty;
    private TextView textLast;
    private MaterialButton buttonBackup;
    private ExecutorService executor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        repository = new NoteRepository(this);
        executor = Executors.newSingleThreadExecutor();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        textEmpty = findViewById(R.id.text_backup_empty);
        textLast = findViewById(R.id.text_last_backup);
        buttonBackup = findViewById(R.id.btn_backup_now);

        RecyclerView listView = findViewById(R.id.list_backups);
        adapter = new BackupAdapter(this);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        buttonBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backupNow();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBackups();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /* ---------------- 备份 ---------------- */

    private void backupNow() {
        buttonBackup.setEnabled(false);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    File file = BackupManager.createBackup(getApplicationContext(), repository);
                    message = getString(R.string.backup_done, BackupManager.readInfo(file).count);
                } catch (IOException e) {
                    message = getString(R.string.backup_failed);
                }
                final String result = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        Toast.makeText(BackupActivity.this, result, Toast.LENGTH_SHORT).show();
                        buttonBackup.setEnabled(true);
                        loadBackups();
                    }
                });
            }
        });
    }

    private void loadBackups() {
        final long last = AppPrefs.getLastBackupTime(this);
        textLast.setText(last == 0 ? getString(R.string.backup_never)
                : getString(R.string.backup_last_time, TimeUtils.formatRelative(last)));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<BackupManager.BackupInfo> list = BackupManager.listBackups(
                        getApplicationContext());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        adapter.submit(list);
                        textEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
    }

    /* ---------------- 恢复 ---------------- */

    @Override
    public void onBackupClick(BackupManager.BackupInfo info) {
        showRestoreOptions(info);
    }

    @Override
    public void onBackupLongClick(BackupManager.BackupInfo info, View anchor) {
        showRestoreOptions(info);
    }

    private void showRestoreOptions(final BackupManager.BackupInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.backup_item_title, TimeUtils.formatSecond(info.time)))
                .setMessage(R.string.backup_restore_confirm_desc)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setNeutralButton(R.string.backup_delete, (dialog, which) -> confirmDelete(info))
                .setPositiveButton(R.string.backup_replace, (dialog, which) ->
                        restoreFromFile(info.file))
                .show();
    }

    private void confirmDelete(final BackupManager.BackupInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_delete)
                .setMessage(R.string.backup_delete_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    if (info.file != null) {
                        //noinspection ResultOfMethodCallIgnored
                        info.file.delete();
                    }
                    loadBackups();
                })
                .show();
    }

    private void restoreFromFile(final File file) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String json;
                try {
                    json = BackupManager.readText(file);
                } catch (IOException e) {
                    toastOnUi(R.string.backup_restore_failed);
                    return;
                }
                doRestore(json);
            }
        });
    }

    private void doRestore(final String json) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final int count = BackupManager.restore(getApplicationContext(), repository,
                            json);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) {
                                return;
                            }
                            Toast.makeText(BackupActivity.this,
                                    getString(R.string.backup_restored, count),
                                    Toast.LENGTH_SHORT).show();
                            loadBackups();
                        }
                    });
                } catch (JSONException e) {
                    toastOnUi(R.string.backup_restore_failed);
                }
            }
        });
    }

    private void toastOnUi(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Toast.makeText(BackupActivity.this, resId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
