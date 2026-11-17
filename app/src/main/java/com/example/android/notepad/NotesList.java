package com.example.android.notepad;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 笔记列表页。
 *
 * <p>扩展点：</p>
 * <ul>
 *   <li>每条笔记显示时间戳（今天 / 昨天 / 日期）</li>
 *   <li>顶部搜索框可按标题或内容查询</li>
 *   <li>分类筛选 Chip、置顶、长按菜单（置顶 / 改分类 / 导出 / 删除）</li>
 * </ul>
 */
public class NotesList extends AppCompatActivity implements NoteAdapter.Listener {

    private NoteRepository repository;
    private NoteAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private TextView countText;
    private EditText searchEdit;
    private ImageButton clearSearch;
    private ChipGroup chipGroup;

    /** 当前选中的分类，null 表示全部 */
    private String currentCategory;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private ExecutorService executor;

    /** 待导出的笔记（系统文件选择器回调时使用） */
    private Note pendingExportNote;

    private ActivityResultLauncher<String> exportNoteLauncher;
    private ActivityResultLauncher<String> exportAllLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        repository = new NoteRepository(this);
        executor = Executors.newSingleThreadExecutor();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        emptyView = findViewById(R.id.empty_view);
        emptyTitle = findViewById(R.id.text_empty_title);
        emptyDesc = findViewById(R.id.text_empty_desc);
        countText = findViewById(R.id.text_count);
        searchEdit = findViewById(R.id.edit_search);
        clearSearch = findViewById(R.id.btn_clear_search);
        chipGroup = findViewById(R.id.chip_group);
        recyclerView = findViewById(R.id.list_notes);

        adapter = new NoteAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NotesList.this, NoteEditor.class));
            }
        });

        setupSearch();

        exportNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null && pendingExportNote != null) {
                        boolean ok = ExportHelper.writeToUri(NotesList.this, uri,
                                ExportHelper.noteToText(NotesList.this, pendingExportNote));
                        Toast.makeText(NotesList.this,
                                ok ? R.string.export_success : R.string.export_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                    pendingExportNote = null;
                });

        exportAllLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/markdown"),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    boolean ok = ExportHelper.writeToUri(this, uri,
                            ExportHelper.notesToMarkdown(this, repository.all()));
                    Toast.makeText(this, ok ? R.string.export_success : R.string.export_failed,
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 分类可能在分类管理页被修改
        if (currentCategory != null && !CategoryManager.get(this).exists(currentCategory)) {
            currentCategory = null;
        }
        buildCategoryChips();
        reload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /* ---------------- 搜索 ---------------- */

    private void setupSearch() {
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEdit.setText("");
            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                // 输入防抖，避免每输入一个字符就查询一次
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        reload();
                    }
                };
                handler.postDelayed(searchRunnable, 220);
            }
        });
    }

    /* ---------------- 分类 Chip ---------------- */

    private void buildCategoryChips() {
        chipGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        Chip all = (Chip) inflater.inflate(R.layout.chip_filter, chipGroup, false);
        all.setId(View.generateViewId());
        all.setText(getString(R.string.category_all));
        all.setTag(null);
        chipGroup.addView(all);
        if (currentCategory == null) {
            all.setChecked(true);
        }

        for (Category category : CategoryManager.get(this).getCategories()) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_filter, chipGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(category.name);
            chip.setTag(category.name);
            chip.setChipIcon(createDot(category.color));
            chip.setChipIconVisible(true);
            chipGroup.addView(chip);
            if (category.name.equals(currentCategory)) {
                chip.setChecked(true);
            }
        }

        chipGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    return;
                }
                View chip = group.findViewById(checkedIds.get(0));
                currentCategory = chip == null ? null : (String) chip.getTag();
                reload();
            }
        });
    }

    private GradientDrawable createDot(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        int size = ThemeHelper.dp(this, 10);
        dot.setSize(size, size);
        return dot;
    }

    /* ---------------- 数据 ---------------- */

    private void reload() {
        String keyword = searchEdit.getText().toString().trim();
        List<Note> notes = repository.query(
                TextUtils.isEmpty(keyword) ? null : keyword,
                currentCategory,
                AppPrefs.getSortOrder(this));

        adapter.submit(notes);
        countText.setText(getString(R.string.notes_count, notes.size()));

        boolean empty = notes.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            boolean filtering = !TextUtils.isEmpty(keyword) || currentCategory != null;
            emptyTitle.setText(filtering ? R.string.empty_search_title : R.string.empty_notes_title);
            emptyDesc.setText(filtering ? R.string.empty_search_desc : R.string.empty_notes_desc);
        }
    }

    /* ---------------- 列表交互 ---------------- */

    @Override
    public void onNoteClick(Note note) {
        openEditor(note);
    }

    private void openEditor(Note note) {
        Intent intent = new Intent(this, NoteEditor.class);
        intent.setData(NotePad.Notes.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(note.id)).build());
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(final Note note, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, note.pinned ? R.string.menu_unpin : R.string.menu_pin);
        popup.getMenu().add(0, 2, 1, R.string.action_change_category);
        popup.getMenu().add(0, 3, 2, R.string.menu_export);
        popup.getMenu().add(0, 4, 3, R.string.menu_delete);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        repository.setPinned(note.id, !note.pinned);
                        reload();
                        return true;
                    case 2:
                        CategoryPicker.show(NotesList.this, note.category,
                                new CategoryPicker.OnCategorySelected() {
                                    @Override
                                    public void onSelected(String name) {
                                        repository.setCategory(note.id, name);
                                        reload();
                                    }
                                });
                        return true;
                    case 3:
                        pendingExportNote = note;
                        exportNoteLauncher.launch(
                                ExportHelper.suggestedFileName(note.title) + ".txt");
                        return true;
                    case 4:
                        confirmDelete(note);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void confirmDelete(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    repository.delete(note.id);
                    reload();
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /* ---------------- 菜单 ---------------- */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes_list, menu);
        ThemeHelper.tintMenuIcons(menu, ThemeHelper.onSurface(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_manage_categories) {
            startActivity(new Intent(this, CategoryActivity.class));
            return true;
        } else if (id == R.id.menu_backup) {
            startActivity(new Intent(this, BackupActivity.class));
            return true;
        } else if (id == R.id.menu_export_all) {
            exportAll();
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportAll() {
        if (repository.countAll() == 0) {
            Toast.makeText(this, R.string.export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        exportAllLauncher.launch(getString(R.string.export_all_file_name) + ".md");
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }
}
