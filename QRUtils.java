package com.uusafe.framework.util.qr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.Validate;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRUtils {

	private static final int SIDE_LENGTH = 230;
	private static final int LOGO_RATIO = 5;
	private static final int FRAME_WIDTH = 2;

	private static MultiFormatWriter mutiWriter = new MultiFormatWriter();

	/**
	 * 根据指定的内容生成二维码,大小采用默认值
	 * @param content 二维码内容
	 * @param target 二维码输出目标文件
	 * @throws WriterException
	 * @throws IOException
	 */
	public static void genBarcode(String content, File target) throws WriterException, IOException {
		genBarcode(content, SIDE_LENGTH, target);
	}

	/**
	 * 根据指定的内容生成二维码,大小有参数指定
	 * @param content 二维码内容
	 * @param target 二维码输出目标文件
	 * @throws WriterException
	 * @throws IOException
	 */
	public static void genBarcode(String content, int sideLength, File target)
			throws WriterException, IOException {
		Validate.notBlank(content);
		Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.MARGIN, 1);
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		BitMatrix matrix = mutiWriter.encode(content, BarcodeFormat.QR_CODE, sideLength, sideLength, hints);
		MatrixToImageWriter.writeToPath(matrix, "jpg", target.toPath());
	}

	/**
	 * 根据指定内容和嵌入的logo生成二维码,大小采用默认值
	 * @param content 二维码内容
	 * @param logo 嵌入二维码中心的logo
	 * @param target 二维码输出目标文件
	 * @throws WriterException
	 * @throws IOException
	 */
	public static void genBarcode(String content, File logo, File target) throws WriterException, IOException {
		genBarcode(content, SIDE_LENGTH, logo, target);
	}

	/**
	 * 根据指定内容和嵌入的logo生成二维码,大小有参数指定
	 * @param content 二维码内容
	 * @param logo 嵌入二维码中心的logo
	 * @param target 二维码输出目标文件
	 * @throws WriterException
	 * @throws IOException
	 */
	public static void genBarcode(String content, int sideLength, File logo, File target)
			throws WriterException, IOException {
		Validate.notBlank(content);
		// 生成二维码
		Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.MARGIN, 1);
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		BitMatrix matrix = mutiWriter.encode(content, BarcodeFormat.QR_CODE, sideLength, sideLength, hints);
		// 获取logo
		int[][] logoPixels = logoScale(logo, sideLength / LOGO_RATIO, sideLength / LOGO_RATIO, false);

		BufferedImage image = merge(matrix, logoPixels, sideLength);
		
		ImageIO.write(image, "jpg", target);
	}
	
	/*
	 * 将图片嵌入二维码中心位置
	 */
	private static BufferedImage merge(BitMatrix matrix, int[][] logoPixels, int sideLength) {
		// 二维矩阵转为一维像素数组
		int halfW = matrix.getWidth() / 2;
		int halfH = matrix.getHeight() / 2;
		int[] pixels = new int[sideLength * sideLength];

		int IMAGE_HALF_WIDTH = sideLength / LOGO_RATIO / 2;
		for (int y = 0; y < matrix.getHeight(); y++) {
			for (int x = 0; x < matrix.getWidth(); x++) {
				// 读取图片
				if (x > halfW - IMAGE_HALF_WIDTH && x < halfW + IMAGE_HALF_WIDTH && y > halfH - IMAGE_HALF_WIDTH
						&& y < halfH + IMAGE_HALF_WIDTH) {
					pixels[y * sideLength + x] = logoPixels[x - halfW + IMAGE_HALF_WIDTH][y - halfH + IMAGE_HALF_WIDTH];
				}
				// 在图片四周形成边框
				else if ((x > halfW - IMAGE_HALF_WIDTH - FRAME_WIDTH && x < halfW - IMAGE_HALF_WIDTH + FRAME_WIDTH
						&& y > halfH - IMAGE_HALF_WIDTH - FRAME_WIDTH && y < halfH + IMAGE_HALF_WIDTH + FRAME_WIDTH)
						|| (x > halfW + IMAGE_HALF_WIDTH - FRAME_WIDTH && x < halfW + IMAGE_HALF_WIDTH + FRAME_WIDTH
								&& y > halfH - IMAGE_HALF_WIDTH - FRAME_WIDTH
								&& y < halfH + IMAGE_HALF_WIDTH + FRAME_WIDTH)
						|| (x > halfW - IMAGE_HALF_WIDTH - FRAME_WIDTH && x < halfW + IMAGE_HALF_WIDTH + FRAME_WIDTH
								&& y > halfH - IMAGE_HALF_WIDTH - FRAME_WIDTH
								&& y < halfH - IMAGE_HALF_WIDTH + FRAME_WIDTH)
						|| (x > halfW - IMAGE_HALF_WIDTH - FRAME_WIDTH && x < halfW + IMAGE_HALF_WIDTH + FRAME_WIDTH
								&& y > halfH + IMAGE_HALF_WIDTH - FRAME_WIDTH
								&& y < halfH + IMAGE_HALF_WIDTH + FRAME_WIDTH)) {
					pixels[y * sideLength + x] = 0xfffffff;
				} else {
					// 此处可以修改二维码的颜色，可以分别制定二维码和背景的颜色；
					pixels[y * sideLength + x] = matrix.get(x, y) ? 0xff000000 : 0xfffffff;
				}
			}
		}

		BufferedImage image = new BufferedImage(sideLength, sideLength, BufferedImage.TYPE_INT_RGB);
		image.getRaster().setDataElements(0, 0, sideLength, sideLength, pixels);
		return image;
	}

	/*
	 * 获取图片数据
	 */
	private static int[][] logoScale(File logo, int width, int height, boolean hasFiller) throws IOException {
		double ratio = 0.0;
		BufferedImage srcImage = ImageIO.read(logo);
		Image destImage = srcImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		if ((srcImage.getHeight() > height) || (srcImage.getWidth() > width)) {
			if (srcImage.getHeight() > srcImage.getWidth()) {
				ratio = (new Integer(height)).doubleValue() / srcImage.getHeight();
			} else {
				ratio = (new Integer(width)).doubleValue() / srcImage.getWidth();
			}
			AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null);
			destImage = op.filter(srcImage, null);
		}
		if (hasFiller) {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphic = image.createGraphics();
			graphic.setColor(Color.white);
			graphic.fillRect(0, 0, width, height);
			if (width == destImage.getWidth(null))
				graphic.drawImage(destImage, 0, (height - destImage.getHeight(null)) / 2, destImage.getWidth(null),
						destImage.getHeight(null), Color.white, null);
			else {
				graphic.drawImage(destImage, (width - destImage.getWidth(null)) / 2, 0, destImage.getWidth(null),
						destImage.getHeight(null), Color.white, null);
			}

			graphic.dispose();
			destImage = image;
		}
		
		BufferedImage scaleImage = (BufferedImage) destImage;
		int[][] logoPixels = new int[width][height];
		for (int i = 0; i < scaleImage.getWidth(); i++) {
			for (int j = 0; j < scaleImage.getHeight(); j++) {
				logoPixels[i][j] = scaleImage.getRGB(i, j);
			}
		}
		return logoPixels;
	}
}
