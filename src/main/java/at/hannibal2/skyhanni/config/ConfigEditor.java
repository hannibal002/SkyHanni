/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.core.GlScissorStack;
import at.hannibal2.skyhanni.config.core.GuiElement;
import at.hannibal2.skyhanni.config.core.GuiElementTextField;
import at.hannibal2.skyhanni.config.core.config.gui.GuiOptionEditor;
import at.hannibal2.skyhanni.config.core.config.gui.GuiOptionEditorAccordion;
import at.hannibal2.skyhanni.config.core.config.struct.ConfigProcessor;
import at.hannibal2.skyhanni.config.core.util.lerp.LerpUtils;
import at.hannibal2.skyhanni.config.core.util.lerp.LerpingInteger;
import at.hannibal2.skyhanni.config.core.util.render.GuiRenderUtils;
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils;
import at.hannibal2.skyhanni.utils.StringUtils;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;

import static at.hannibal2.skyhanni.config.GuiTextures.DISCORD;
import static at.hannibal2.skyhanni.config.GuiTextures.GITHUB;

public class ConfigEditor extends GuiElement {
    private static final ResourceLocation[] socialsIco = new ResourceLocation[]{
            DISCORD, GITHUB
    };
    private static final String[] socialsLink = new String[]{
            "https://discord.gg/8DXVN4BJz3",
            "https://github.com/hannibal00212/SkyHanni"
    };
    private static final ResourceLocation SEARCH_ICON = new ResourceLocation("notenoughupdates:core/search.png");
    public static ConfigEditor editor = new ConfigEditor(SkyHanniMod.feature);
    private final long openedMillis;
    private final LerpingInteger optionsScroll = new LerpingInteger(0, 150);
    private final LerpingInteger categoryScroll = new LerpingInteger(0, 150);
    private final LinkedHashMap<String, ConfigProcessor.ProcessedCategory> processedConfig;
    private final TreeMap<String, Set<ConfigProcessor.ProcessedOption>> searchOptionMap = new TreeMap<>();
    private final HashMap<ConfigProcessor.ProcessedOption, ConfigProcessor.ProcessedCategory> categoryForOption =
            new HashMap<>();
    private final LerpingInteger minimumSearchSize = new LerpingInteger(0, 150);
    private final GuiElementTextField searchField = new GuiElementTextField("", 0, 20, 0);
    private String selectedCategory = null;
    private Set<ConfigProcessor.ProcessedCategory> searchedCategories = null;
    private Map<ConfigProcessor.ProcessedCategory, Set<Integer>> searchedAccordions = null;
    private Set<ConfigProcessor.ProcessedOption> searchedOptions = null;
    private float optionsBarStart;
    private float optionsBarend;
    private int lastMouseX = 0;
    private int keyboardScrollXCutoff = 0;

    public ConfigEditor(Features config) {
        this(config, null);
    }

    public ConfigEditor(Features config, String categoryOpen) {
        this.openedMillis = System.currentTimeMillis();
        this.processedConfig = ConfigProcessor.create(config);

        for (ConfigProcessor.ProcessedCategory category : processedConfig.values()) {
            for (ConfigProcessor.ProcessedOption option : category.options.values()) {
                categoryForOption.put(option, category);

                String combined = category.name + " " + category.desc + " " + option.name + " " + option.desc;
                combined = combined.replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
                for (String word : combined.split("[ _]")) {
                    searchOptionMap.computeIfAbsent(word, k -> new HashSet<>()).add(option);
                }
            }
        }

        if (categoryOpen != null) {
            for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                if (category.getValue().name.equalsIgnoreCase(categoryOpen)) {
                    selectedCategory = category.getKey();
                    break;
                }
            }
            if (selectedCategory == null) {
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                    if (category.getValue().name.toLowerCase().startsWith(categoryOpen.toLowerCase())) {
                        selectedCategory = category.getKey();
                        break;
                    }
                }
            }
            if (selectedCategory == null) {
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                    if (category.getValue().name.toLowerCase().contains(categoryOpen.toLowerCase())) {
                        selectedCategory = category.getKey();
                        break;
                    }
                }
            }
        }

        editor = this;
    }

    private LinkedHashMap<String, ConfigProcessor.ProcessedCategory> getCurrentConfigEditing() {
        LinkedHashMap<String, ConfigProcessor.ProcessedCategory> newMap = new LinkedHashMap<>(processedConfig);
        if (searchedCategories != null) newMap.values().retainAll(searchedCategories);
        return newMap;
    }

    private LinkedHashMap<String, ConfigProcessor.ProcessedOption> getOptionsInCategory(ConfigProcessor.ProcessedCategory cat) {
        LinkedHashMap<String, ConfigProcessor.ProcessedOption> newMap = new LinkedHashMap<>(cat.options);

        if (searchedOptions != null) {
            Set<ConfigProcessor.ProcessedOption> retain = new HashSet<>();
            retain.addAll(searchedOptions);

            if (searchedAccordions != null) {
                Set<Integer> visibleAccordions = searchedAccordions.get(cat);

                if (visibleAccordions != null && !visibleAccordions.isEmpty()) {
                    for (ConfigProcessor.ProcessedOption option : newMap.values()) {
                        if (option.editor instanceof GuiOptionEditorAccordion) {
                            int accordionId = ((GuiOptionEditorAccordion) option.editor).getAccordionId();

                            if (visibleAccordions.contains(accordionId)) {
                                retain.add(option);
                            }
                        }
                    }
                }

            }

            newMap.values().retainAll(retain);
        }
        return newMap;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    private void setSelectedCategory(String category) {
        selectedCategory = category;
        optionsScroll.setValue(0);
    }

    public String getSelectedCategoryName() {
        return processedConfig.get(selectedCategory).name;
    }

    public void search() {
        String search = searchField.getText().trim().replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
        searchedCategories = null;
        searchedOptions = null;
        searchedAccordions = null;

        if (!search.isEmpty()) {
            searchedCategories = new HashSet<>();
            searchedAccordions = new HashMap<>();

            for (String word : search.split(" ")) {
                if (word.trim().isEmpty()) continue;

                Set<ConfigProcessor.ProcessedOption> options = new HashSet<>();

                Map<String, Set<ConfigProcessor.ProcessedOption>> map = StringUtils.INSTANCE.subMapWithKeysThatAreSuffixes(word, searchOptionMap);

                map.values().forEach(options::addAll);

                if (!options.isEmpty()) {
                    if (searchedOptions == null) {
                        searchedOptions = new HashSet<>(options);
                    } else {
                        searchedOptions.retainAll(options);
                    }
                }
            }

            if (searchedOptions == null) {
                searchedOptions = new HashSet<>();
            } else {
                for (ConfigProcessor.ProcessedOption option : searchedOptions) {
                    ConfigProcessor.ProcessedCategory cat = categoryForOption.get(option);
                    if (cat == null) continue;

                    searchedCategories.add(cat);
                    searchedAccordions.computeIfAbsent(cat, k -> new HashSet<>()).add(option.accordionId);
                }
            }
        }
    }

    public void render() {
        optionsScroll.tick();
        categoryScroll.tick();
        handleKeyboardPresses();

        List<String> tooltipToDisplay = null;

        long currentTime = System.currentTimeMillis();
        long delta = currentTime - openedMillis;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        float opacityFactor = LerpUtils.sigmoidZeroOne(delta / 500f);
        GuiRenderUtils.drawGradientRect(0, 0, 0, width, height,
                (int) (0x80 * opacityFactor) << 24 | 0x101010,
                (int) (0x90 * opacityFactor) << 24 | 0x101010
        );

        int xSize = Math.min(scaledResolution.getScaledWidth() - 100 / scaledResolution.getScaleFactor(), 500);
        int ySize = Math.min(scaledResolution.getScaledHeight() - 100 / scaledResolution.getScaleFactor(), 400);

        int x = (scaledResolution.getScaledWidth() - xSize) / 2;
        int y = (scaledResolution.getScaledHeight() - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int openingXSize = xSize;
        int openingYSize = ySize;
        if (delta < 150) {
            openingXSize = (int) (delta * xSize / 150);
            openingYSize = 5;
        } else if (delta < 300) {
            openingYSize = 5 + (int) (delta - 150) * (ySize - 5) / 150;
        }
        GuiRenderUtils.drawFloatingRectDark(
                (scaledResolution.getScaledWidth() - openingXSize) / 2,
                (scaledResolution.getScaledHeight() - openingYSize) / 2,
                openingXSize, openingYSize
        );
        GlScissorStack.clear();
        GlScissorStack.push((scaledResolution.getScaledWidth() - openingXSize) / 2,
                (scaledResolution.getScaledHeight() - openingYSize) / 2,
                (scaledResolution.getScaledWidth() + openingXSize) / 2,
                (scaledResolution.getScaledHeight() + openingYSize) / 2, scaledResolution
        );

        GuiRenderUtils.drawFloatingRectDark(x + 5, y + 5, xSize - 10, 20, false);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        TextRenderUtils.drawStringCenteredScaledMaxWidth("SkyHanni " + SkyHanniMod.VERSION + " by " + EnumChatFormatting.RED + "hannibal2" + EnumChatFormatting.RESET + ", config by " + EnumChatFormatting.DARK_PURPLE + "Moulberry", fr, x + xSize / 2f, y + 15, false, 200, 0xa0a0a0);
        GuiRenderUtils.drawFloatingRectDark(x + 4, y + 49 - 20,
                140, ySize - 54 + 20, false
        );

        int innerPadding = 20 / adjScaleFactor;
        int innerLeft = x + 4 + innerPadding;
        int innerRight = x + 144 - innerPadding;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        Gui.drawRect(innerLeft, innerTop, innerLeft + 1, innerBottom, 0xff08080E); //Left
        Gui.drawRect(innerLeft + 1, innerTop, innerRight, innerTop + 1, 0xff08080E); //Top
        Gui.drawRect(innerRight - 1, innerTop + 1, innerRight, innerBottom, 0xff28282E); //Right
        Gui.drawRect(innerLeft + 1, innerBottom - 1, innerRight - 1, innerBottom, 0xff28282E); //Bottom
        Gui.drawRect(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, 0x6008080E); //Middle

        GlScissorStack.push(0, innerTop + 1, scaledResolution.getScaledWidth(),
                innerBottom - 1, scaledResolution
        );

        float catBarSize = 1;
        int catY = -categoryScroll.getValue();

        LinkedHashMap<String, ConfigProcessor.ProcessedCategory> currentConfigEditing = getCurrentConfigEditing();
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : currentConfigEditing.entrySet()) {
            String selectedCategory = getSelectedCategory();
            if (selectedCategory == null || !currentConfigEditing.containsKey(selectedCategory)) {
                setSelectedCategory(entry.getKey());
            }
            String catName = entry.getValue().name;
            if (entry.getKey().equals(getSelectedCategory())) {
                catName = EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.UNDERLINE + catName;
            } else {
                catName = EnumChatFormatting.GRAY + catName;
            }
            TextRenderUtils.drawStringCenteredScaledMaxWidth(catName,
                    fr, x + 75, y + 70 + catY, false, 100, -1
            );
            catY += 15;
            if (catY > 0) {
                catBarSize =
                        LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (catY + 5 + categoryScroll.getValue()));
            }
        }

        float catBarStart = categoryScroll.getValue() / (float) (catY + categoryScroll.getValue());
        float catBarEnd = catBarStart + catBarSize;
        if (catBarEnd > 1) {
            catBarEnd = 1;
            if (categoryScroll.getTarget() / (float) (catY + categoryScroll.getValue()) + catBarSize < 1) {
                int target = optionsScroll.getTarget();
                categoryScroll.setValue((int) Math.ceil(
                        (catY + 5 + categoryScroll.getValue()) - catBarSize * (catY + 5 + categoryScroll.getValue())));
                categoryScroll.setTarget(target);
            } else {
                categoryScroll.setValue((int) Math.ceil(
                        (catY + 5 + categoryScroll.getValue()) - catBarSize * (catY + 5 + categoryScroll.getValue())));
            }
        }
        int catDist = innerBottom - innerTop - 12;
        Gui.drawRect(innerLeft + 2, innerTop + 5, innerLeft + 7, innerBottom - 5, 0xff101010);
        Gui.drawRect(innerLeft + 3, innerTop + 6 + (int) (catDist * catBarStart), innerLeft + 6,
                innerTop + 6 + (int) (catDist * catBarEnd), 0xff303030
        );

        GlScissorStack.pop(scaledResolution);

        TextRenderUtils.drawStringCenteredScaledMaxWidth("Categories",
                fr, x + 75, y + 44, false, 120, 0xa368ef
        );

        GuiRenderUtils.drawFloatingRectDark(x + 149, y + 29, xSize - 154, ySize - 34, false);

        innerLeft = x + 149 + innerPadding;
        innerRight = x + xSize - 5 - innerPadding;
        innerBottom = y + ySize - 5 - innerPadding;

        Minecraft.getMinecraft().getTextureManager().bindTexture(SEARCH_ICON);
        GlStateManager.color(1, 1, 1, 1);
        GuiRenderUtils.drawTexturedRect(innerRight - 20, innerTop - (20 + innerPadding) / 2 - 9, 18, 18, GL11.GL_NEAREST);

        minimumSearchSize.tick();
        boolean shouldShow = !searchField.getText().trim().isEmpty() || searchField.getFocus();
        if (shouldShow && minimumSearchSize.getTarget() < 30) {
            minimumSearchSize.setTarget(30);
            minimumSearchSize.resetTimer();
        } else if (!shouldShow && minimumSearchSize.getTarget() > 0) {
            minimumSearchSize.setTarget(0);
            minimumSearchSize.resetTimer();
        }

        int rightStuffLen = 20;
        if (minimumSearchSize.getValue() > 1) {
            int strLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(searchField.getText()) + 10;
            if (!shouldShow) strLen = 0;

            int len = Math.max(strLen, minimumSearchSize.getValue());
            searchField.setSize(len, 18);
            searchField.render(innerRight - 25 - len, innerTop - (20 + innerPadding) / 2 - 9);

            rightStuffLen += 5 + len;
        }

        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());

            TextRenderUtils.drawStringScaledMaxWidth(cat.desc,
                    fr, innerLeft + 5, y + 40, true, innerRight - innerLeft - rightStuffLen - 10, 0xb0b0b0
            );
        }

        Gui.drawRect(innerLeft, innerTop, innerLeft + 1, innerBottom, 0xff08080E); //Left
        Gui.drawRect(innerLeft + 1, innerTop, innerRight, innerTop + 1, 0xff08080E); //Top
        Gui.drawRect(innerRight - 1, innerTop + 1, innerRight, innerBottom, 0xff303036); //Right
        Gui.drawRect(innerLeft + 1, innerBottom - 1, innerRight - 1, innerBottom, 0xff303036); //Bottom
        Gui.drawRect(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, 0x6008080E); //Middle

        GlScissorStack.push(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, scaledResolution);
        float barSize = 1;
        int optionY = -optionsScroll.getValue();
        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;
            GlStateManager.enableDepth();
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                    int accordionDepth = activeAccordions.get(option.accordionId);
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                int optionHeight = editor.getHeight();
                if (innerTop + 5 + optionY + optionHeight > innerTop + 1 && innerTop + 5 + optionY < innerBottom - 1) {
                    editor.render((innerLeft + innerRight - optionWidth) / 2 - 5, innerTop + 5 + optionY, optionWidth);
                }
                optionY += optionHeight + 5;
            }
            GlStateManager.disableDepth();
            if (optionY > 0) {
                barSize =
                        LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (optionY + 5 + optionsScroll.getValue()));
            }
        }

        GlScissorStack.pop(scaledResolution);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            int optionYOverlay = -optionsScroll.getValue();
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;

            GlStateManager.translate(0, 0, 10);
            GlStateManager.enableDepth();
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                    int accordionDepth = activeAccordions.get(option.accordionId);
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                int optionHeight = editor.getHeight();
                if (innerTop + 5 + optionYOverlay + optionHeight > innerTop + 1 &&
                        innerTop + 5 + optionYOverlay < innerBottom - 1) {
                    editor.renderOverlay(
                            (innerLeft + innerRight - optionWidth) / 2 - 5,
                            innerTop + 5 + optionYOverlay,
                            optionWidth
                    );
                }
                optionYOverlay += optionHeight + 5;
            }
            GlStateManager.disableDepth();
            GlStateManager.translate(0, 0, -10);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        optionsBarStart = optionsScroll.getValue() / (float) (optionY + optionsScroll.getValue());
        optionsBarend = optionsBarStart + barSize;
        if (optionsBarend > 1) {
            optionsBarend = 1;
            if (optionsScroll.getTarget() / (float) (optionY + optionsScroll.getValue()) + barSize < 1) {
                int target = optionsScroll.getTarget();
                optionsScroll.setValue((int) Math.ceil(
                        (optionY + 5 + optionsScroll.getValue()) - barSize * (optionY + 5 + optionsScroll.getValue())));
                optionsScroll.setTarget(target);
            } else {
                optionsScroll.setValue((int) Math.ceil(
                        (optionY + 5 + optionsScroll.getValue()) - barSize * (optionY + 5 + optionsScroll.getValue())));
            }
        }
        int dist = innerBottom - innerTop - 12;
        Gui.drawRect(innerRight - 10, innerTop + 5, innerRight - 5, innerBottom - 5, 0xff101010);
        Gui.drawRect(
                innerRight - 9,
                innerTop + 6 + (int) (dist * optionsBarStart),
                innerRight - 6,
                innerTop + 6 + (int) (dist * optionsBarend),
                0xff303030
        );

        for (int socialIndex = 0; socialIndex < socialsIco.length; socialIndex++) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(socialsIco[socialIndex]);
            GlStateManager.color(1, 1, 1, 1);
            int socialLeft = x + xSize - 23 - 18 * socialIndex;
            GuiRenderUtils.drawTexturedRect(socialLeft, y + 7, 16, 16, GL11.GL_LINEAR);

            if (mouseX >= socialLeft && mouseX <= socialLeft + 16 &&
                    mouseY >= y + 6 && mouseY <= y + 23) {
                tooltipToDisplay = Lists.newArrayList(
                        EnumChatFormatting.YELLOW + "Go to: " + EnumChatFormatting.RESET + socialsLink[socialIndex]);
            }
        }

        GlScissorStack.clear();

        if (tooltipToDisplay != null) {
            TextRenderUtils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, fr);
        }

        GlStateManager.translate(0, 0, -2);
    }

    public boolean mouseInput(int mouseX, int mouseY) {
        lastMouseX = mouseX;
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        int xSize = Math.min(width - 100 / scaledResolution.getScaleFactor(), 500);
        int ySize = Math.min(height - 100 / scaledResolution.getScaleFactor(), 400);

        int x = (scaledResolution.getScaledWidth() - xSize) / 2;
        int y = (scaledResolution.getScaledHeight() - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int innerPadding = 20 / adjScaleFactor;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        int innerLeft = x + 149 + innerPadding;
        int innerRight = x + xSize - 5 - innerPadding;

        int dist = innerBottom - innerTop - 12;
        int optionsBarStartY = innerTop + 6 + (int) (dist * optionsBarStart);
        int optionsBarEndY = innerTop + 6 + (int) (dist * optionsBarend);
        int optionsBarStartX = innerRight - 12;
        int optionsBarEndX = innerRight - 3;

        int categoryY = -categoryScroll.getValue();
        categoryY += 15 * getCurrentConfigEditing().size();
        int catDist = innerBottom - innerTop - 12;
        float catBarStart = categoryScroll.getValue() / (float) (categoryY + categoryScroll.getValue());
        float categoryBarSize = LerpUtils.clampZeroOne(
                (float) (innerBottom - innerTop - 2) / (categoryY + 5 + categoryScroll.getValue()));
        float catBarEnd = catBarStart + categoryBarSize;
        int categoryBarStartY = innerTop + 6 + (int) (catDist * catBarStart);
        int categoryBarEndY = innerTop + 6 + (int) (catDist * catBarEnd);
        int categoryBarStartX = x + innerPadding + 7;
        int categoryBarEndX = x + innerPadding + 12;
        keyboardScrollXCutoff = innerLeft - 10;
        if (Mouse.getEventButtonState()) {
            if ((mouseY < optionsBarStartY || mouseY > optionsBarEndY) &&
                    (mouseX >= optionsBarStartX && mouseX <= optionsBarEndX) && mouseY > innerTop + 6 && mouseY < innerBottom - 6) {
                optionsScroll.setTimeToReachTarget(200);
                optionsScroll.resetTimer();
                optionsScroll.setTarget(mouseY - innerTop);
                return true;
            } else if ((mouseY < categoryBarStartY || mouseY > categoryBarEndY) &&
                    (mouseX >= categoryBarStartX && mouseX <= categoryBarEndX) && mouseY > innerTop + 6 &&
                    mouseY < innerBottom - 6) {
                categoryScroll.setTimeToReachTarget(200);
                categoryScroll.resetTimer();
                categoryScroll.setTarget(mouseY - innerTop);
                return true;
            }

            searchField.setFocus(mouseX >= innerRight - 20 && mouseX <= innerRight - 2 &&
                    mouseY >= innerTop - (20 + innerPadding) / 2 - 9 && mouseY <= innerTop - (20 + innerPadding) / 2 + 9);

            if (minimumSearchSize.getValue() > 1) {
                int strLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(searchField.getText()) + 10;
                int len = Math.max(strLen, minimumSearchSize.getValue());

                if (mouseX >= innerRight - 25 - len && mouseX <= innerRight - 25 &&
                        mouseY >= innerTop - (20 + innerPadding) / 2 - 9 && mouseY <= innerTop - (20 + innerPadding) / 2 + 9) {
                    String old = searchField.getText();
                    searchField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());

                    if (!searchField.getText().equals(old)) search();
                }
            }
        }

        int dWheel = Mouse.getEventDWheel();
        if (mouseY > innerTop && mouseY < innerBottom && dWheel != 0) {
            if (dWheel < 0) {
                dWheel = -1;
            }
            if (dWheel > 0) {
                dWheel = 1;
            }
            if (mouseX < innerLeft) {
                int newTarget = categoryScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) {
                    newTarget = 0;
                }

                float catBarSize = 1;
                int catY = -newTarget;
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
                    if (getSelectedCategory() == null) {
                        setSelectedCategory(entry.getKey());
                    }

                    catY += 15;
                    if (catY > 0) {
                        catBarSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (catY + 5 + newTarget));
                    }
                }

                int barMax = (int) Math.floor((catY + 5 + newTarget) - catBarSize * (catY + 5 + newTarget));
                if (newTarget > barMax) {
                    newTarget = barMax;
                }
                categoryScroll.resetTimer();
                categoryScroll.setTarget(newTarget);
            } else {
                int newTarget = optionsScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) {
                    newTarget = 0;
                }

                float barSize = 1;
                int optionY = -newTarget;
                if (getSelectedCategory() != null && getCurrentConfigEditing() != null &&
                        getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                    ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                    HashMap<Integer, Integer> activeAccordions = new HashMap<>();
                    for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                        if (option.accordionId >= 0) {
                            if (!activeAccordions.containsKey(option.accordionId)) {
                                continue;
                            }
                        }

                        GuiOptionEditor editor = option.editor;
                        if (editor == null) {
                            continue;
                        }
                        if (editor instanceof GuiOptionEditorAccordion) {
                            GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                            if (accordion.getToggled()) {
                                int accordionDepth = 0;
                                if (option.accordionId >= 0) {
                                    accordionDepth = activeAccordions.get(option.accordionId) + 1;
                                }
                                activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                            }
                        }
                        optionY += editor.getHeight() + 5;

                        if (optionY > 0) {
                            barSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (optionY + 5 + newTarget));
                        }
                    }
                }

                int barMax = (int) Math.floor((optionY + 5 + newTarget) - barSize * (optionY + 5 + newTarget));
                if (newTarget > barMax) {
                    newTarget = barMax;
                }
                optionsScroll.setTimeToReachTarget(Math.min(
                        150,
                        Math.max(10, 5 * Math.abs(newTarget - optionsScroll.getValue()))
                ));
                optionsScroll.resetTimer();
                optionsScroll.setTarget(newTarget);
            }
        } else if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            if (getCurrentConfigEditing() != null) {
                int catY = -categoryScroll.getValue();
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
                    if (getSelectedCategory() == null) {
                        setSelectedCategory(entry.getKey());
                    }
                    if (mouseX >= x + 5 && mouseX <= x + 145 &&
                            mouseY >= y + 70 + catY - 7 && mouseY <= y + 70 + catY + 7) {
                        setSelectedCategory(entry.getKey());
                        return true;
                    }
                    catY += 15;
                }
            }

            for (int socialIndex = 0; socialIndex < socialsLink.length; socialIndex++) {
                int socialLeft = x + xSize - 23 - 18 * socialIndex;

                if (mouseX >= socialLeft && mouseX <= socialLeft + 16 &&
                        mouseY >= y + 6 && mouseY <= y + 23) {
                    try {
                        Desktop.getDesktop().browse(new URI(socialsLink[socialIndex]));
                    } catch (Exception ignored) {
                    }
                    return true;
                }
            }
        }

        int optionY = -optionsScroll.getValue();
        if (getSelectedCategory() != null && getCurrentConfigEditing() != null &&
                getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            int optionWidthDefault = innerRight - innerLeft - 20;
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                    int accordionDepth = activeAccordions.get(option.accordionId);
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                if (editor.mouseInputOverlay(
                        (innerLeft + innerRight - optionWidth) / 2 - 5,
                        innerTop + 5 + optionY,
                        optionWidth,
                        mouseX,
                        mouseY
                )) {
                    return true;
                }
                optionY += editor.getHeight() + 5;
            }
        }

        if (mouseX > innerLeft && mouseX < innerRight &&
                mouseY > innerTop && mouseY < innerBottom) {
            optionY = -optionsScroll.getValue();
            if (getSelectedCategory() != null && getCurrentConfigEditing() != null &&
                    getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                int optionWidthDefault = innerRight - innerLeft - 20;
                ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                HashMap<Integer, Integer> activeAccordions = new HashMap<>();
                for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                    int optionWidth = optionWidthDefault;
                    if (option.accordionId >= 0) {
                        if (!activeAccordions.containsKey(option.accordionId)) {
                            continue;
                        }
                        int accordionDepth = activeAccordions.get(option.accordionId);
                        optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                    }

                    GuiOptionEditor editor = option.editor;
                    if (editor == null) {
                        continue;
                    }
                    if (editor instanceof GuiOptionEditorAccordion) {
                        GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                        if (accordion.getToggled()) {
                            int accordionDepth = 0;
                            if (option.accordionId >= 0) {
                                accordionDepth = activeAccordions.get(option.accordionId) + 1;
                            }
                            activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                        }
                    }
                    if (editor.mouseInput(
                            (innerLeft + innerRight - optionWidth) / 2 - 5,
                            innerTop + 5 + optionY,
                            optionWidth,
                            mouseX,
                            mouseY
                    )) {
                        return true;
                    }
                    optionY += editor.getHeight() + 5;
                }
            }
        }

        return true;
    }

    public boolean keyboardInput() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();

        int xSize = Math.min(width - 100 / scaledResolution.getScaleFactor(), 500);

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int innerPadding = 20 / adjScaleFactor;
        int innerWidth = xSize - 154 - innerPadding * 2;

        if (Keyboard.getEventKeyState()) {
            String old = searchField.getText();
            searchField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            searchField.setText(Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(
                    searchField.getText(),
                    innerWidth / 2 - 20
            ));

            if (!searchField.getText().equals(old)) search();
        }

        if (getSelectedCategory() != null && getCurrentConfigEditing() != null &&
                getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                if (editor.keyboardInput()) {
                    return true;
                }
            }
        }

        return true;
    }

    private void handleKeyboardPresses() {
        LerpingInteger target = lastMouseX < keyboardScrollXCutoff ? categoryScroll : optionsScroll;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            target.setTarget(target.getTarget() + 5);
        } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            if (target.getTarget() >= 0) {
                target.setTarget(target.getTarget() - 5);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            SkyHanniMod.configManager.saveConfig();
        }
    }
}
