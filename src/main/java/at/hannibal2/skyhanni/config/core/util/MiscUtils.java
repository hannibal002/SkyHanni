///*
// * Copyright (C) 2022 NotEnoughUpdates contributors
// *
// * This file is part of NotEnoughUpdates.
// *
// * NotEnoughUpdates is free software: you can redistribute it
// * and/or modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation, either
// * version 3 of the License, or (at your option) any later version.
// *
// * NotEnoughUpdates is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
// */
//
//package at.hannibal2.skyhanni.config.core.util;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.util.ResourceLocation;
//import org.lwjgl.BufferUtils;
//import org.lwjgl.input.Cursor;
//import org.lwjgl.input.Mouse;
//
//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.datatransfer.StringSelection;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.IntBuffer;
//import java.nio.file.Files;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//
//public class MiscUtils {
//	public static void copyToClipboard(String str) {
//		Toolkit.getDefaultToolkit().getSystemClipboard()
//					 .setContents(new StringSelection(str), null);
//	}
//
//	private static void unzip(InputStream src, File dest) {
//		//buffer for read and write data to file
//		byte[] buffer = new byte[1024];
//		try {
//			ZipInputStream zis = new ZipInputStream(src);
//			ZipEntry ze = zis.getNextEntry();
//			while (ze != null) {
//				if (!ze.isDirectory()) {
//					String fileName = ze.getName();
//					File newFile = new File(dest, fileName);
//					//create directories for sub directories in zip
//					new File(newFile.getParent()).mkdirs();
//					FileOutputStream fos = new FileOutputStream(newFile);
//					int len;
//					while ((len = zis.read(buffer)) > 0) {
//						fos.write(buffer, 0, len);
//					}
//					fos.close();
//				}
//				//close this ZipEntry
//				zis.closeEntry();
//				ze = zis.getNextEntry();
//			}
//			//close last ZipEntry
//			zis.closeEntry();
//			zis.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void recursiveDelete(File file) {
//		if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
//			for (File child : file.listFiles()) {
//				recursiveDelete(child);
//			}
//		}
//		file.delete();
//	}
//
//	private static String currentCursor = null;
//
//	public static void resetCursor() {
//		if (currentCursor == null) {
//			return;
//		}
//		currentCursor = null;
//		try {
//			Mouse.setNativeCursor(null);
//		} catch (Exception ignored) {
//		}
//	}
//
//	public static void setCursor(ResourceLocation loc, int hotspotX, int hotspotY) {
//		if (currentCursor != null && loc.getResourcePath().equals(currentCursor)) {
//			return;
//		}
//		currentCursor = loc.getResourcePath();
//		try {
//			BufferedImage image = ImageIO.read(Minecraft.getMinecraft()
//																									.getResourceManager().getResource(loc).getInputStream());
//			int maxSize = Cursor.getMaxCursorSize();
//			IntBuffer buffer = BufferUtils.createIntBuffer(maxSize * maxSize);
//			for (int i = 0; i < maxSize * maxSize; i++) {
//				int cursorX = i % maxSize;
//				int cursorY = i / maxSize;
//				if (cursorX >= image.getWidth() || cursorY >= image.getHeight()) {
//					buffer.put(0x00000000);
//				} else {
//					buffer.put(image.getRGB(cursorX, image.getHeight() - 1 - cursorY));
//				}
//			}
//			buffer.flip();
//			Mouse.setNativeCursor(new Cursor(maxSize, maxSize, hotspotX, hotspotY, 1,
//				buffer, null
//			));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//}
