package com.example.android.notepad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * 分类选择底部弹窗（编辑器与列表长按菜单共用）。
 */
public class CategoryPicker {

    /** 选择回调 */
    public interface OnCategorySelected {
        void onSelected(String name);
    }

    private CategoryPicker() {
    }

    public static void show(final Activity activity, String current,
                            final OnCategorySelected callback) {
        View content = activity.getLayoutInflater()
                .inflate(R.layout.sheet_category_picker, null, false);
        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(content);

        final ChipGroup group = content.findViewById(R.id.chip_group_picker);
        LayoutInflater inflater = LayoutInflater.from(activity);

        for (Category category : CategoryManager.get(activity).getCategories()) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_filter, group, false);
            chip.setId(View.generateViewId());
            chip.setText(category.name);
            chip.setTag(category.name);
            chip.setChipIcon(dot(activity, category.color));
            chip.setChipIconVisible(true);
            group.addView(chip);
            if (category.name.equals(current)) {
                chip.setChecked(true);
            }
        }

        group.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup chipGroup, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    return;
                }
                View chip = chipGroup.findViewById(checkedIds.get(0));
                if (chip != null && chip.getTag() != null) {
                    callback.onSelected((String) chip.getTag());
                    dialog.dismiss();
                }
            }
        });

        content.findViewById(R.id.btn_manage_categories).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                activity.startActivity(new Intent(activity, CategoryActivity.class));
            }
        });

        dialog.show();
    }

    private static GradientDrawable dot(Activity activity, int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        int size = ThemeHelper.dp(activity, 10);
        dot.setSize(size, size);
        return dot;
    }
}
