package com.example.android.notepad;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * 笔记编辑页。
 *
 * <p>支持：标题 / 正文编辑、分类切换、置顶、分享、导出、删除、自动保存，
 * 以及字号 / 行距 / 纸张背景等个性化设置。</p>
 */
public class NoteEditor extends AppCompatActivity {

    private NoteRepository repository;

    private long noteId = -1;
    private long createdTime;
    private long modifiedTime;
    private String category = "";
    private boolean pinned;
    private boolean dirty;
    private boolean deleted;
    /** 笔记是否成功载入（避免载入失败时把内容覆盖成空） */
    private boolean loaded = true;

    private EditText editTitle;
    private EditText editContent;
    private TextView textMeta;
    private TextView textWordCount;
    private Chip chipCategory;
    private MaterialToolbar toolbar;

    private ActivityResultLauncher<String> exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        repository = new NoteRepository(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveIfNeeded(true);
                finish();
            }
        });

        editTitle = findViewById(R.id.edit_title);
        editContent = findViewById(R.id.edit_content);
        textMeta = findViewById(R.id.text_meta);
        textWordCount = findViewById(R.id.text_word_count);
        chipCategory = findViewById(R.id.chip_category);

        applyEditorStyle();
        loadNote();
        updateCategoryChip();
        updateWordCount();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dirty = true;
                updateWordCount();
            }
        };
        editTitle.addTextChangedListener(watcher);
        editContent.addTextChangedListener(watcher);

        chipCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CategoryPicker.show(NoteEditor.this, category,
                        new CategoryPicker.OnCategorySelected() {
                            @Override
                            public void onSelected(String name) {
                                category = name;
                                dirty = true;
                                updateCategoryChip();
                            }
                        });
            }
        });

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null) {
                        boolean ok = ExportHelper.writeToUri(this, uri,
                                ExportHelper.noteToText(this, currentNote()));
                        Toast.makeText(this, ok ? R.string.export_success : R.string.export_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** 应用偏好的字号、行距与纸张背景 */
    private void applyEditorStyle() {
        int fontSize = AppPrefs.getFontSize(this);
        editContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        float extra = fontSize * scaledDensity
                * ((AppPrefs.getLineSpacing(this) - 100) / 100f);
        editContent.setLineSpacing(extra, 1f);

        View scroll = findViewById(R.id.scroll_editor);
        scroll.setBackgroundColor(ThemeHelper.editorBackgroundColor(this));
    }

    /** 载入已有笔记，或为新建笔记设定默认值 */
    private void loadNote() {
        Intent intent = getIntent();
        Uri data = intent == null ? null : intent.getData();
        if (data != null) {
            List<String> segments = data.getPathSegments();
            if (segments.size() >= 2) {
                try {
                    noteId = Long.parseLong(segments.get(1));
                } catch (NumberFormatException ignored) {
                    noteId = -1;
                }
            }
        }

        if (noteId > 0) {
            Note note = repository.find(noteId);
            if (note != null) {
                createdTime = note.created;
                modifiedTime = note.modified;
                pinned = note.pinned;
                category = note.category;
                editTitle.setText(note.title);
                editContent.setText(note.content);
                editTitle.setSelection(note.title.length());
                editContent.setSelection(note.content.length());
                textMeta.setText(getString(R.string.editor_meta,
                        TimeUtils.formatFull(createdTime), TimeUtils.formatFull(modifiedTime)));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.editor_title_edit);
                }
            } else {
                noteId = -1;
                loaded = false;
                finish();
                return;
            }
        } else {
            createdTime = System.currentTimeMillis();
            modifiedTime = createdTime;
            category = AppPrefs.getDefaultCategory(this);
            if (TextUtils.isEmpty(category) || !CategoryManager.get(this).exists(category)) {
                List<Category> categories = CategoryManager.get(this).getCategories();
                category = categories.isEmpty()
                        ? CategoryManager.UNCATEGORIZED : categories.get(0).name;
            }
            textMeta.setText(getString(R.string.editor_updated,
                    TimeUtils.formatFull(createdTime)));
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.editor_title_new);
            }
        }
        dirty = false;
    }

    private void updateCategoryChip() {
        Category item = CategoryManager.get(this).find(category);
        String name = item != null ? item.name : CategoryManager.UNCATEGORIZED;
        int color = item != null ? item.color : CategoryManager.get(this).colorOf(category);
        boolean dark = ThemeHelper.isDark(this);

        chipCategory.setText(name);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        int size = ThemeHelper.dp(this, 10);
        dot.setSize(size, size);
        chipCategory.setChipIcon(dot);
        chipCategory.setChipIconVisible(true);
        chipCategory.setChipBackgroundColor(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color, dark ? 0x33 : 0x1F)));
        chipCategory.setTextColor(dark
                ? ColorUtils.blendARGB(color, Color.WHITE, 0.4f) : color);
    }

    private void updateWordCount() {
        String content = editContent.getText().toString();
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                count++;
            }
        }
        textWordCount.setText(getString(R.string.editor_word_count, count));
    }

    private Note currentNote() {
        Note note = new Note();
        note.id = noteId;
        note.title = editTitle.getText().toString().trim();
        note.content = editContent.getText().toString();
        note.category = category;
        note.pinned = pinned;
        note.created = createdTime;
        note.modified = modifiedTime;
        return note;
    }

    /* ---------------- 保存 ---------------- */

    private boolean saveIfNeeded(boolean showToast) {
        if (deleted || !loaded) {
            return false;
        }
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString();

        if (noteId <= 0) {
            if (title.isEmpty() && content.trim().isEmpty()) {
                return false;
            }
            noteId = repository.insert(title, content, category);
            if (noteId > 0) {
                if (pinned) {
                    repository.setPinned(noteId, true);
                }
                createdTime = System.currentTimeMillis();
                modifiedTime = createdTime;
                dirty = false;
                textMeta.setText(getString(R.string.editor_meta,
                        TimeUtils.formatFull(createdTime), TimeUtils.formatFull(modifiedTime)));
                if (showToast) {
                    Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }

        if (!dirty) {
            return false;
        }
        repository.save(noteId, title, content, category, pinned);
        modifiedTime = System.currentTimeMillis();
        dirty = false;
        textMeta.setText(getString(R.string.editor_meta,
                TimeUtils.formatFull(createdTime), TimeUtils.formatFull(modifiedTime)));
        if (showToast) {
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AppPrefs.isAutoSave(this)) {
            saveIfNeeded(false);
        }
    }

    @Override
    public void onBackPressed() {
        saveIfNeeded(false);
        super.onBackPressed();
    }

    /* ---------------- 菜单 ---------------- */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        ThemeHelper.tintMenuIcons(menu, ThemeHelper.onSurface(this));
        MenuItem pinItem = menu.findItem(R.id.menu_pin);
        if (pinItem != null) {
            pinItem.setTitle(pinned ? R.string.menu_unpin : R.string.menu_pin);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            saveIfNeeded(true);
            finish();
            return true;
        } else if (id == R.id.menu_pin) {
            pinned = !pinned;
            dirty = true;
            if (noteId > 0) {
                repository.setPinned(noteId, pinned);
            }
            invalidateOptionsMenu();
            Toast.makeText(this, pinned ? R.string.menu_pin : R.string.menu_unpin,
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_share) {
            Note note = currentNote();
            ExportHelper.shareText(this,
                    TextUtils.isEmpty(note.title) ? getString(R.string.untitled) : note.title,
                    ExportHelper.noteToText(this, note));
            return true;
        } else if (id == R.id.menu_export) {
            Note note = currentNote();
            exportLauncher.launch(ExportHelper.suggestedFileName(note.title) + ".txt");
            return true;
        } else if (id == R.id.menu_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        if (noteId <= 0) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    deleted = true;
                    repository.delete(noteId);
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }
}
