package com.example.android.notepad;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类管理页：新增 / 重命名 / 改色 / 删除分类。
 */
public class CategoryActivity extends AppCompatActivity implements CategoryAdapter.Listener {

    /** 可选颜色 */
    private static final int[] PALETTE = new int[]{
            0xFF5B7FFF, 0xFFFF9F43, 0xFF26C6DA, 0xFFAB47BC,
            0xFFEF5350, 0xFF66BB6A, 0xFF8D6E63, 0xFF78909C,
            0xFFEC407A, 0xFF26A69A
    };

    private NoteRepository repository;
    private CategoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        repository = new NoteRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.list_categories);
        emptyView = findViewById(R.id.text_empty);
        adapter = new CategoryAdapter(repository, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_category);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditDialog(null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Category> categories = CategoryManager.get(this).getCategories();
        adapter.submit(categories);
        emptyView.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCategoryClick(Category category) {
        showEditDialog(category);
    }

    @Override
    public void onCategoryLongClick(Category category, View anchor) {
        showOptions(category);
    }

    private void showOptions(final Category category) {
        String[] items = new String[]{
                getString(R.string.category_rename),
                getString(R.string.dialog_delete)
        };
        new AlertDialog.Builder(this)
                .setTitle(category.name)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(category);
                    } else {
                        confirmDelete(category);
                    }
                })
                .show();
    }

    /**
     * 新增 / 编辑分类对话框（名称 + 颜色）
     */
    private void showEditDialog(final Category existing) {
        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_category_edit, null, false);

        final TextInputLayout inputLayout = content.findViewById(R.id.input_layout);
        final TextInputEditText editName = content.findViewById(R.id.edit_name);
        LinearLayout colorRow = content.findViewById(R.id.color_row);

        final int[] selectedColor = {existing != null ? existing.color : PALETTE[0]};
        if (existing != null) {
            editName.setText(existing.name);
            if (editName.getText() != null) {
                editName.setSelection(editName.getText().length());
            }
        }

        final List<View> dots = new ArrayList<>();
        for (final int color : PALETTE) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ThemeHelper.dp(this, 34), ThemeHelper.dp(this, 34));
            params.setMarginEnd(ThemeHelper.dp(this, 8));
            dot.setLayoutParams(params);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor[0] = color;
                    updateDots(dots, selectedColor[0]);
                }
            });
            colorRow.addView(dot);
            dots.add(dot);
        }
        updateDots(dots, selectedColor[0]);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.category_add : R.string.category_edit_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_ok, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = editName.getText() == null
                                ? "" : editName.getText().toString().trim();
                        if (name.isEmpty()) {
                            inputLayout.setError(getString(R.string.category_name_empty));
                            return;
                        }
                        CategoryManager manager = CategoryManager.get(CategoryActivity.this);
                        if (existing == null) {
                            if (!manager.add(name, selectedColor[0])) {
                                inputLayout.setError(getString(R.string.category_exists));
                                return;
                            }
                            Toast.makeText(CategoryActivity.this, R.string.category_added,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            if (!manager.rename(existing.name, name, selectedColor[0])) {
                                inputLayout.setError(getString(R.string.category_exists));
                                return;
                            }
                            if (!existing.name.equals(name)) {
                                // 分类改名后同步迁移笔记
                                repository.reassignCategory(existing.name, name);
                            } else {
                                manager.updateColor(name, selectedColor[0]);
                            }
                            Toast.makeText(CategoryActivity.this, R.string.category_renamed,
                                    Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                        reload();
                    }
                }));

        dialog.show();
    }

    private void updateDots(List<View> dots, int selectedColor) {
        for (int i = 0; i < dots.size(); i++) {
            int color = PALETTE[i];
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            if (color == selectedColor) {
                drawable.setStroke(ThemeHelper.dp(this, 2), ThemeHelper.onSurface(this));
            }
            dots.get(i).setBackground(drawable);
        }
    }

    private void confirmDelete(final Category category) {
        int count = repository.countByCategory(category.name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete)
                .setMessage(getString(R.string.category_delete_message, count))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    if (!CategoryManager.get(CategoryActivity.this).remove(category.name)) {
                        Toast.makeText(CategoryActivity.this, R.string.category_cannot_delete,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.reassignCategory(category.name, CategoryManager.UNCATEGORIZED);
                    Toast.makeText(CategoryActivity.this, R.string.category_deleted,
                            Toast.LENGTH_SHORT).show();
                    reload();
                })
                .show();
    }
}
