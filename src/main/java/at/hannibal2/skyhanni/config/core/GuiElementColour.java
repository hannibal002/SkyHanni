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

package at.hannibal2.skyhanni.config.core;

import at.hannibal2.skyhanni.config.core.util.render.GuiRenderUtils;
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class GuiElementColour extends GuiElement {
	public static final ResourceLocation colour_selector_dot = new ResourceLocation(
		"notenoughupdates:core/colour_selector_dot.png");
	public static final ResourceLocation colour_selector_bar = new ResourceLocation(
		"notenoughupdates:core/colour_selector_bar.png");
	public static final ResourceLocation colour_selector_bar_alpha = new ResourceLocation(
		"notenoughupdates:core/colour_selector_bar_alpha.png");
	public static final ResourceLocation colour_selector_chroma = new ResourceLocation(
		"notenoughupdates:core/colour_selector_chroma.png");

	private static final ResourceLocation colourPickerLocation = new ResourceLocation("mbcore:dynamic/colourpicker");
	private static final ResourceLocation colourPickerBarValueLocation = new ResourceLocation(
		"mbcore:dynamic/colourpickervalue");
	private static final ResourceLocation colourPickerBarOpacityLocation = new ResourceLocation(
		"mbcore:dynamic/colourpickeropacity");
	private final GuiElementTextField hexField = new GuiElementTextField(
		"",
		GuiElementTextField.SCALE_TEXT | GuiElementTextField.FORCE_CAPS | GuiElementTextField.NO_SPACE
	);

	private final int x;
	private final int y;
	private int xSize = 119;
	private final int ySize = 89;

	private float wheelAngle = 0;
	private float wheelRadius = 0;

	private int clickedComponent = -1;

	private final Consumer<String> colourChangedCallback;
	private final Runnable closeCallback;
	private String colour;

	private final boolean opacitySlider;
	private final boolean valueSlider;

	public GuiElementColour(
		int x, int y, String initialColour, Consumer<String> colourChangedCallback,
		Runnable closeCallback
	) {
		this(x, y, initialColour, colourChangedCallback, closeCallback, true, true);
	}

	public GuiElementColour(
		int x, int y, String initialColour, Consumer<String> colourChangedCallback,
		Runnable closeCallback, boolean opacitySlider, boolean valueSlider
	) {
		final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

		this.y = Math.max(10, Math.min(scaledResolution.getScaledHeight() - ySize - 10, y));
		this.x = Math.max(10, Math.min(scaledResolution.getScaledWidth() - xSize - 10, x));

		this.colour = initialColour;
		this.colourChangedCallback = colourChangedCallback;
		this.closeCallback = closeCallback;

		int colour = ChromaColour.specialToSimpleRGB(initialColour);
		Color c = new Color(colour);
		float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
		updateAngleAndRadius(hsv);

		this.opacitySlider = opacitySlider;
		this.valueSlider = valueSlider;

		if (!valueSlider) xSize -= 15;
		if (!opacitySlider) xSize -= 15;
	}

	public void updateAngleAndRadius(float[] hsv) {
		this.wheelRadius = hsv[1];
		this.wheelAngle = hsv[0] * 360;
	}

	public void render() {
		GuiRenderUtils.drawFloatingRectDark(x, y, xSize, ySize);

		int currentColour = ChromaColour.specialToSimpleRGB(colour);
		Color c = new Color(currentColour, true);
		float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

		BufferedImage bufferedImage = new BufferedImage(288, 288, BufferedImage.TYPE_INT_ARGB);
		float borderRadius = 0.05f;
		if (Keyboard.isKeyDown(Keyboard.KEY_N)) borderRadius = 0;
		for (int x = -16; x < 272; x++) {
			for (int y = -16; y < 272; y++) {
				float radius = (float) Math.sqrt(((x - 128) * (x - 128) + (y - 128) * (y - 128)) / 16384f);
				float angle = (float) Math.toDegrees(Math.atan((128 - x) / (y - 128 + 1E-5)) + Math.PI / 2);
				if (y < 128) angle += 180;
				if (radius <= 1) {
					int rgb = Color.getHSBColor(angle / 360f, (float) Math.pow(radius, 1.5f), hsv[2]).getRGB();
					bufferedImage.setRGB(x + 16, y + 16, rgb);
				} else if (radius <= 1 + borderRadius) {
					float invBlackAlpha = Math.abs(radius - 1 - borderRadius / 2) / borderRadius * 2;
					float blackAlpha = 1 - invBlackAlpha;

					if (radius > 1 + borderRadius / 2) {
						bufferedImage.setRGB(x + 16, y + 16, (int) (blackAlpha * 255) << 24);
					} else {
						Color col = Color.getHSBColor(angle / 360f, 1, hsv[2]);
						int rgb = (int) (col.getRed() * invBlackAlpha) << 16 |
							(int) (col.getGreen() * invBlackAlpha) << 8 |
							(int) (col.getBlue() * invBlackAlpha);
						bufferedImage.setRGB(x + 16, y + 16, 0xff000000 | rgb);
					}

				}
			}
		}

		BufferedImage bufferedImageValue = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 64; y++) {
				if ((x == 0 || x == 9) && (y == 0 || y == 63)) continue;

				int rgb = Color.getHSBColor(wheelAngle / 360, wheelRadius, (64 - y) / 64f).getRGB();
				bufferedImageValue.setRGB(x, y, rgb);
			}
		}

		BufferedImage bufferedImageOpacity = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 64; y++) {
				if ((x == 0 || x == 9) && (y == 0 || y == 63)) continue;

				int rgb = (currentColour & 0x00FFFFFF) | (Math.min(255, (64 - y) * 4) << 24);
				bufferedImageOpacity.setRGB(x, y, rgb);
			}
		}

		float selradius = (float) Math.pow(wheelRadius, 1 / 1.5f) * 32;
		int selx = (int) (Math.cos(Math.toRadians(wheelAngle)) * selradius);
		int sely = (int) (Math.sin(Math.toRadians(wheelAngle)) * selradius);

		int valueOffset = 0;
		if (valueSlider) {
			valueOffset = 15;

			Minecraft.getMinecraft().getTextureManager().loadTexture(
				colourPickerBarValueLocation,
				new DynamicTexture(bufferedImageValue)
			);
			Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerBarValueLocation);
			GlStateManager.color(1, 1, 1, 1);
			GuiRenderUtils.drawTexturedRect(x + 5 + 64 + 5, y + 5, 10, 64, GL11.GL_NEAREST);
		}

		int opacityOffset = 0;
		if (opacitySlider) {
			opacityOffset = 15;

			Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_bar_alpha);
			GlStateManager.color(1, 1, 1, 1);
			GuiRenderUtils.drawTexturedRect(x + 5 + 64 + 5 + valueOffset, y + 5, 10, 64, GL11.GL_NEAREST);

			Minecraft.getMinecraft().getTextureManager().loadTexture(
				colourPickerBarOpacityLocation,
				new DynamicTexture(bufferedImageOpacity)
			);
			Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerBarOpacityLocation);
			GlStateManager.color(1, 1, 1, 1);
			GuiRenderUtils.drawTexturedRect(x + 5 + 64 + 5 + valueOffset, y + 5, 10, 64, GL11.GL_NEAREST);
		}

		int chromaSpeed = ChromaColour.getSpeed(colour);
		int currentColourChroma = ChromaColour.specialToChromaRGB(colour);
		Color cChroma = new Color(currentColourChroma, true);
		float[] hsvChroma = Color.RGBtoHSB(cChroma.getRed(), cChroma.getGreen(), cChroma.getBlue(), null);

		if (chromaSpeed > 0) {
			Gui.drawRect(x + 5 + 64 + valueOffset + opacityOffset + 5 + 1, y + 5 + 1,
				x + 5 + 64 + valueOffset + opacityOffset + 5 + 10 - 1, y + 5 + 64 - 1,
				Color.HSBtoRGB(hsvChroma[0], 0.8f, 0.8f)
			);
		} else {
			Gui.drawRect(x + 5 + 64 + valueOffset + opacityOffset + 5 + 1, y + 5 + 27 + 1,
				x + 5 + 64 + valueOffset + opacityOffset + 5 + 10 - 1, y + 5 + 37 - 1,
				Color.HSBtoRGB((hsvChroma[0] + (System.currentTimeMillis() - ChromaColour.startTime) / 1000f) % 1, 0.8f, 0.8f)
			);
		}

		Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_bar);
		GlStateManager.color(1, 1, 1, 1);
		if (valueSlider) GuiRenderUtils.drawTexturedRect(x + 5 + 64 + 5, y + 5, 10, 64, GL11.GL_NEAREST);
		if (opacitySlider) GuiRenderUtils.drawTexturedRect(x + 5 + 64 + 5 + valueOffset, y + 5, 10, 64, GL11.GL_NEAREST);

		if (chromaSpeed > 0) {
			GuiRenderUtils.drawTexturedRect(x + 5 + 64 + valueOffset + opacityOffset + 5, y + 5, 10, 64, GL11.GL_NEAREST);
		} else {
			Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_chroma);
			GuiRenderUtils.drawTexturedRect(x + 5 + 64 + valueOffset + opacityOffset + 5, y + 5 + 27, 10, 10, GL11.GL_NEAREST);
		}

		if (valueSlider) Gui.drawRect(x + 5 + 64 + 5, y + 5 + 64 - (int) (64 * hsv[2]),
			x + 5 + 64 + valueOffset, y + 5 + 64 - (int) (64 * hsv[2]) + 1, 0xFF000000
		);
		if (opacitySlider) Gui.drawRect(x + 5 + 64 + 5 + valueOffset, y + 5 + 64 - c.getAlpha() / 4,
			x + 5 + 64 + valueOffset + opacityOffset, y + 5 + 64 - c.getAlpha() / 4 - 1, 0xFF000000
		);
		if (chromaSpeed > 0) {
			Gui.drawRect(x + 5 + 64 + valueOffset + opacityOffset + 5,
				y + 5 + 64 - (int) (chromaSpeed / 255f * 64),
				x + 5 + 64 + valueOffset + opacityOffset + 5 + 10,
				y + 5 + 64 - (int) (chromaSpeed / 255f * 64) + 1, 0xFF000000
			);
		}

		Minecraft.getMinecraft().getTextureManager().loadTexture(colourPickerLocation, new DynamicTexture(bufferedImage));
		Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerLocation);
		GlStateManager.color(1, 1, 1, 1);
		GuiRenderUtils.drawTexturedRect(x + 1, y + 1, 72, 72, GL11.GL_LINEAR);

		Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_dot);
		GlStateManager.color(1, 1, 1, 1);
		GuiRenderUtils.drawTexturedRect(x + 5 + 32 + selx - 4, y + 5 + 32 + sely - 4, 8, 8, GL11.GL_NEAREST);

		TextRenderUtils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY.toString() + Math.round(hsv[2] * 100) + "",
			Minecraft.getMinecraft().fontRendererObj,
			x + 5 + 64 + 5 + 5 - (Math.round(hsv[2] * 100) == 100 ? 1 : 0), y + 5 + 64 + 5 + 5, true, 13, -1
		);
		if (opacitySlider) {
			TextRenderUtils.drawStringCenteredScaledMaxWidth(
				EnumChatFormatting.GRAY.toString() + Math.round(c.getAlpha() / 255f * 100) + "",
				Minecraft.getMinecraft().fontRendererObj,
				x + 5 + 64 + 5 + valueOffset + 5,
				y + 5 + 64 + 5 + 5,
				true,
				13,
				-1
			);
		}
		if (chromaSpeed > 0) {
			TextRenderUtils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY.toString() +
					(int) ChromaColour.getSecondsForSpeed(chromaSpeed) + "s",
				Minecraft.getMinecraft().fontRendererObj,
				x + 5 + 64 + 5 + valueOffset + opacityOffset + 6, y + 5 + 64 + 5 + 5, true, 13, -1
			);
		}

		hexField.setSize(48, 10);
		if (!hexField.getFocus()) hexField.setText(Integer.toHexString(c.getRGB() & 0xFFFFFF).toUpperCase());

		StringBuilder sb = new StringBuilder(EnumChatFormatting.GRAY + "#");
		for (int i = 0; i < 6 - hexField.getText().length(); i++) {
			sb.append("0");
		}
		sb.append(EnumChatFormatting.WHITE);

		hexField.setPrependText(sb.toString());
		hexField.render(x + 5 + 8, y + 5 + 64 + 5);
	}

	public boolean mouseInput(int mouseX, int mouseY) {
		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		float mouseXF = (float) (Mouse.getX() * scaledResolution.getScaledWidth_double() /
			Minecraft.getMinecraft().displayWidth);
		float mouseYF = (float) (scaledResolution.getScaledHeight_double() - Mouse.getY() *
			scaledResolution.getScaledHeight_double() / Minecraft.getMinecraft().displayHeight - 1);

		if ((Mouse.getEventButton() == 0 || Mouse.getEventButton() == 1) && Mouse.getEventButtonState()) {
			if (mouseX > x + 5 + 8 && mouseX < x + 5 + 8 + 48) {
				if (mouseY > y + 5 + 64 + 5 && mouseY < y + 5 + 64 + 5 + 10) {
					hexField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
					clickedComponent = -1;
					return true;
				}
			}
		}
		if (!Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
			clickedComponent = -1;
		}
		if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
			if (mouseX >= x && mouseX <= x + 119 &&
				mouseY >= y && mouseY <= y + 89) {
				hexField.unfocus();

				int xWheel = mouseX - x - 5;
				int yWheel = mouseY - y - 5;

				if (xWheel > 0 && xWheel < 64) {
					if (yWheel > 0 && yWheel < 64) {
						clickedComponent = 0;
					}
				}

				int xValue = mouseX - (x + 5 + 64 + 5);
				int y = mouseY - this.y - 5;

				int opacityOffset = opacitySlider ? 15 : 0;
				int valueOffset = valueSlider ? 15 : 0;

				if (y > -5 && y <= 69) {
					if (valueSlider) {
						if (xValue > 0 && xValue < 10) {
							clickedComponent = 1;
						}
					}

					if (opacitySlider) {
						int xOpacity = mouseX - (x + 5 + 64 + 5 + valueOffset);

						if (xOpacity > 0 && xOpacity < 10) {
							clickedComponent = 2;
						}
					}
				}

				int chromaSpeed = ChromaColour.getSpeed(colour);
				int xChroma = mouseX - (x + 5 + 64 + valueOffset + opacityOffset + 5);
				if (xChroma > 0 && xChroma < 10) {
					if (chromaSpeed > 0) {
						if (y > -5 && y <= 69) {
							clickedComponent = 3;
						}
					} else if (mouseY > this.y + 5 + 27 && mouseY < this.y + 5 + 37) {
						int currentColour = ChromaColour.specialToSimpleRGB(colour);
						Color c = new Color(currentColour, true);
						colour = ChromaColour.special(200, c.getAlpha(), currentColour);
						colourChangedCallback.accept(colour);
					}
				}
			} else {
				hexField.unfocus();
				closeCallback.run();
				return false;
			}
		}
		if (Mouse.isButtonDown(0) && clickedComponent >= 0) {
			int currentColour = ChromaColour.specialToSimpleRGB(colour);
			Color c = new Color(currentColour, true);
			float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

			float xWheel = mouseXF - x - 5;
			float yWheel = mouseYF - y - 5;

			if (clickedComponent == 0) {
				float angle = (float) Math.toDegrees(Math.atan((32 - xWheel) / (yWheel - 32 + 1E-5)) + Math.PI / 2);
				xWheel = Math.max(0, Math.min(64, xWheel));
				yWheel = Math.max(0, Math.min(64, yWheel));
				float radius = (float) Math.sqrt(((xWheel - 32) * (xWheel - 32) + (yWheel - 32) * (yWheel - 32)) / 1024f);
				if (yWheel < 32) angle += 180;

				this.wheelAngle = angle;
				this.wheelRadius = (float) Math.pow(Math.min(1, radius), 1.5f);
				int rgb = Color.getHSBColor(angle / 360f, wheelRadius, hsv[2]).getRGB();
				colour = ChromaColour.special(ChromaColour.getSpeed(colour), c.getAlpha(), rgb);
				colourChangedCallback.accept(colour);
				return true;
			}

			float y = mouseYF - this.y - 5;
			y = Math.max(0, Math.min(64, y));
			System.out.println(y);

			if (clickedComponent == 1) {
				int rgb = Color.getHSBColor(wheelAngle / 360, wheelRadius, 1 - y / 64f).getRGB();
				colour = ChromaColour.special(ChromaColour.getSpeed(colour), c.getAlpha(), rgb);
				colourChangedCallback.accept(colour);
				return true;
			}

			if (clickedComponent == 2) {
				colour = ChromaColour.special(ChromaColour.getSpeed(colour),
					255 - Math.round(y / 64f * 255), currentColour
				);
				colourChangedCallback.accept(colour);
				return true;
			}

			if (clickedComponent == 3) {
				colour = ChromaColour.special(255 - Math.round(y / 64f * 255), c.getAlpha(), currentColour);
				colourChangedCallback.accept(colour);
			}
			return true;
		}
		return false;
	}

	public boolean keyboardInput() {
		if (Keyboard.getEventKeyState() && hexField.getFocus()) {
			if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
				hexField.unfocus();
				return true;
			}
			String old = hexField.getText();

			hexField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());

			if (hexField.getText().length() > 6) {
				hexField.setText(old);
			} else {
				try {
					String text = hexField.getText().toLowerCase();

					int rgb = Integer.parseInt(text, 16);
					int alpha = (ChromaColour.specialToSimpleRGB(colour) >> 24) & 0xFF;
					colour = ChromaColour.special(ChromaColour.getSpeed(colour), alpha, rgb);
					colourChangedCallback.accept(colour);

					Color c = new Color(rgb);
					float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					updateAngleAndRadius(hsv);
				} catch (Exception ignored) {
				}
			}

			return true;
		}
		return false;
	}
}
