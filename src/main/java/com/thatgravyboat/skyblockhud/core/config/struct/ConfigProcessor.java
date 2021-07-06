package com.thatgravyboat.skyblockhud.core.config.struct;

import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud.core.config.Config;
import com.thatgravyboat.skyblockhud.core.config.Config;
import com.thatgravyboat.skyblockhud.core.config.annotations.*;
import com.thatgravyboat.skyblockhud.core.config.gui.*;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;

public class ConfigProcessor {

    public static class ProcessedCategory {

        public final String name;
        public final String desc;
        public final LinkedHashMap<String, ProcessedOption> options = new LinkedHashMap<>();

        public ProcessedCategory(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    public static class ProcessedOption {

        public final String name;
        public final String desc;
        public final int subcategoryId;
        public GuiOptionEditor editor;

        public int accordionId = -1;

        private final Field field;
        private final Object container;

        public ProcessedOption(String name, String desc, int subcategoryId, Field field, Object container) {
            this.name = name;
            this.desc = desc;
            this.subcategoryId = subcategoryId;

            this.field = field;
            this.container = container;
        }

        public Object get() {
            try {
                return field.get(container);
            } catch (Exception e) {
                return null;
            }
        }

        public boolean set(Object value) {
            try {
                if (field.getType() == int.class && value instanceof Number) {
                    field.set(container, ((Number) value).intValue());
                } else {
                    field.set(container, value);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static LinkedHashMap<String, ProcessedCategory> create(Config config) {
        LinkedHashMap<String, ProcessedCategory> processedConfig = new LinkedHashMap<>();
        for (Field categoryField : config.getClass().getDeclaredFields()) {
            boolean exposePresent = categoryField.isAnnotationPresent(Expose.class);
            boolean categoryPresent = categoryField.isAnnotationPresent(Category.class);

            if (exposePresent && categoryPresent) {
                Object categoryObj;
                try {
                    categoryObj = categoryField.get(config);
                } catch (Exception e) {
                    //System.err.printf("Failed to load config category %s. Field was not accessible.\n", categoryField.getName());
                    continue;
                }

                Category categoryAnnotation = categoryField.getAnnotation(Category.class);
                ProcessedCategory cat = new ProcessedCategory(categoryAnnotation.name(), categoryAnnotation.desc());
                processedConfig.put(categoryField.getName(), cat);

                for (Field optionField : categoryObj.getClass().getDeclaredFields()) {
                    boolean optionPresent = optionField.isAnnotationPresent(ConfigOption.class);

                    if (optionPresent) {
                        ConfigOption optionAnnotation = optionField.getAnnotation(ConfigOption.class);
                        ProcessedOption option = new ProcessedOption(optionAnnotation.name(), optionAnnotation.desc(), optionAnnotation.subcategoryId(), optionField, categoryObj);
                        if (optionField.isAnnotationPresent(ConfigAccordionId.class)) {
                            ConfigAccordionId annotation = optionField.getAnnotation(ConfigAccordionId.class);
                            option.accordionId = annotation.id();
                        }

                        GuiOptionEditor editor = null;
                        Class<?> optionType = optionField.getType();
                        if (optionField.isAnnotationPresent(ConfigEditorButton.class)) {
                            ConfigEditorButton configEditorAnnotation = optionField.getAnnotation(ConfigEditorButton.class);
                            editor = new GuiOptionEditorButton(option, configEditorAnnotation.runnableId(), configEditorAnnotation.buttonText(), config);
                        }
                        if (optionType.isAssignableFrom(boolean.class) && optionField.isAnnotationPresent(ConfigEditorBoolean.class)) {
                            editor = new GuiOptionEditorBoolean(option);
                        }
                        if (optionType.isAssignableFrom(boolean.class) && optionField.isAnnotationPresent(ConfigEditorAccordion.class)) {
                            ConfigEditorAccordion configEditorAnnotation = optionField.getAnnotation(ConfigEditorAccordion.class);
                            editor = new GuiOptionEditorAccordion(option, configEditorAnnotation.id());
                        }
                        if (optionType.isAssignableFrom(int.class)) {
                            if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                                ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                                editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), (int) option.get(), true);
                            }
                        }
                        if (optionType.isAssignableFrom(List.class)) {
                            if (optionField.isAnnotationPresent(ConfigEditorDraggableList.class)) {
                                ConfigEditorDraggableList configEditorAnnotation = optionField.getAnnotation(ConfigEditorDraggableList.class);
                                editor = new GuiOptionEditorDraggableList(option, configEditorAnnotation.exampleText());
                            }
                        }
                        if (optionType.isAssignableFrom(String.class)) {
                            if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                                ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                                editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), configEditorAnnotation.initialIndex(), false);
                            } else if (optionField.isAnnotationPresent(ConfigEditorColour.class)) {
                                editor = new GuiOptionEditorColour(option);
                            } else if (optionField.isAnnotationPresent(ConfigEditorText.class)) {
                                editor = new GuiOptionEditorText(option);
                            }
                        }
                        if (optionType.isAssignableFrom(int.class) || optionType.isAssignableFrom(float.class) || optionType.isAssignableFrom(double.class)) {
                            if (optionField.isAnnotationPresent(ConfigEditorSlider.class)) {
                                ConfigEditorSlider configEditorAnnotation = optionField.getAnnotation(ConfigEditorSlider.class);
                                editor = new GuiOptionEditorSlider(option, configEditorAnnotation.minValue(), configEditorAnnotation.maxValue(), configEditorAnnotation.minStep());
                            }
                        }
                        if (optionType.isAssignableFrom(String.class)) {
                            if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                                ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                                editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), 0, false);
                            }
                        }
                        if (editor == null) {
                            //System.err.printf("Failed to load config option %s. Could not find suitable editor.\n", optionField.getName());
                            continue;
                        }
                        option.editor = editor;
                        cat.options.put(optionField.getName(), option);
                    }
                }
            } else if (exposePresent || categoryPresent) {
                //System.err.printf("Failed to load config category %s. Both @Expose and @Category must be present.\n", categoryField.getName());
            }
        }
        return processedConfig;
    }
}
