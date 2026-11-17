package com.example.android.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * 偏好设置页：主题、编辑器排版、列表排序、自动保存 / 自动备份、数据管理。
 */
public class SettingsActivity extends AppCompatActivity {

    private NoteRepository repository;

    private ChipGroup chipTheme;
    private TextView textNoteCount;

    /** 初始化期间不响应选择变化，避免重复 recreate */
    private boolean initializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repository = new NoteRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        chipTheme = findViewById(R.id.chip_theme);
        textNoteCount = findViewById(R.id.text_note_count);

        setupTheme();
        setupList();
        setupData();

        initializing = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        textNoteCount.setText(getString(R.string.settings_note_count, repository.countAll()));
    }

    /* ---------------- 外观 ---------------- */

    private void setupTheme() {
        switch (AppPrefs.getTheme(this)) {
            case AppPrefs.THEME_DARK:
                chipTheme.check(R.id.chip_theme_dark);
                break;
            case AppPrefs.THEME_GREEN:
                chipTheme.check(R.id.chip_theme_green);
                break;
            case AppPrefs.THEME_OCEAN:
                chipTheme.check(R.id.chip_theme_ocean);
                break;
            case AppPrefs.THEME_LIGHT:
            default:
                chipTheme.check(R.id.chip_theme_light);
                break;
        }

        chipTheme.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (initializing || checkedIds.isEmpty()) {
                    return;
                }
                int id = checkedIds.get(0);
                int theme = AppPrefs.THEME_LIGHT;
                if (id == R.id.chip_theme_dark) {
                    theme = AppPrefs.THEME_DARK;
                } else if (id == R.id.chip_theme_green) {
                    theme = AppPrefs.THEME_GREEN;
                } else if (id == R.id.chip_theme_ocean) {
                    theme = AppPrefs.THEME_OCEAN;
                }
                if (theme == AppPrefs.getTheme(SettingsActivity.this)) {
                    return;
                }
                AppPrefs.setTheme(SettingsActivity.this, theme);
                recreate();
            }
        });
    }

    /* ---------------- 列表 ---------------- */

    private void setupList() {
        RadioGroup group = findViewById(R.id.radio_group_sort);
        switch (AppPrefs.getSort(this)) {
            case AppPrefs.SORT_CREATED:
                group.check(R.id.radio_sort_created);
                break;
            case AppPrefs.SORT_TITLE:
                group.check(R.id.radio_sort_title);
                break;
            case AppPrefs.SORT_MODIFIED:
            default:
                group.check(R.id.radio_sort_modified);
                break;
        }

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (initializing) {
                    return;
                }
                int sort = AppPrefs.SORT_MODIFIED;
                if (checkedId == R.id.radio_sort_created) {
                    sort = AppPrefs.SORT_CREATED;
                } else if (checkedId == R.id.radio_sort_title) {
                    sort = AppPrefs.SORT_TITLE;
                }
                AppPrefs.setSort(SettingsActivity.this, sort);
            }
        });
    }

    /* ---------------- 数据 ---------------- */

    private void setupData() {
        findViewById(R.id.row_backup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, BackupActivity.class));
            }
        });

        findViewById(R.id.row_clear_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.settings_clear_all)
                        .setMessage(R.string.settings_clear_all_message)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                            repository.deleteAll();
                            Toast.makeText(SettingsActivity.this, R.string.settings_cleared,
                                    Toast.LENGTH_SHORT).show();
                            textNoteCount.setText(getString(R.string.settings_note_count, 0));
                        })
                        .show();
            }
        });

        findViewById(R.id.row_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.settings_reset)
                        .setMessage(R.string.settings_reset_message)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                            AppPrefs.reset(SettingsActivity.this);
                            CategoryManager.get(SettingsActivity.this).reset();
                            Toast.makeText(SettingsActivity.this, R.string.settings_reset_done,
                                    Toast.LENGTH_SHORT).show();
                            recreate();
                        })
                        .show();
            }
        });

        findViewById(R.id.row_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.about_title)
                        .setMessage(R.string.about_message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
            }
        });
    }
}
