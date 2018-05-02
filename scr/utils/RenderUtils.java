package utils;

import geometry.Face;
import geometry.PolygonPoint;
import geometry.Vector;
import obj.ObjModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static utils.LinearUtils.*;

/**
 * Created by pb on 12.04.2018.
 */
public class RenderUtils {
    public static final int IMAGE_WIDTH = 1024;
    public static final int IMAGE_HEIGHT = 1024;

    private static int getX(double x) {
        return (int) (x * IMAGE_WIDTH / 3.) + IMAGE_WIDTH / 2;
    }

    private static int getY(double y) {
        return (int) (y * IMAGE_HEIGHT / 3.) + IMAGE_HEIGHT / 2;
    }

    private static void setPixel(int x, int y, BufferedImage image, int color, boolean inv) {
        if (inv) {
            int tmp = x;
            x = y;
            y = tmp;
        }
        image.setRGB(x, IMAGE_HEIGHT - 1 - y, color);
    }

    private static void setPixel(int x, int y, BufferedImage image, int color) {
        setPixel(x, y, image, color, false);
    }

    private static void lineBr(int x0, int y0, int x1, int y1, BufferedImage image, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int tmp;
        boolean inv = false;
        if (abs(dy) > abs(dx)) {
            inv = true;
            tmp = x0;
            x0 = y0;
            y0 = tmp;
            tmp = x1;
            x1 = y1;
            y1 = tmp;
            tmp = dx;
            dx = dy;
            dy = tmp;
        }

        if (x1 < x0) {
            tmp = x1;
            x1 = x0;
            x0 = tmp;
            tmp = y1;
            y1 = y0;
            y0 = tmp;
        }

        if (dx == 0) {
            tmp = y1;
            y1 = y0;
            y0 = tmp;
            for (int y = y0; y <= y1; y++) {
                setPixel(x0, y, image, color, inv);
            }
            return;
        }

        float k = abs(1.0f * dy / dx);
        float offset = 0;
        int y = y0;

        for (int x = x0; x <= x1; x++) {
            setPixel(x, y, image, color, inv);
            offset += k;
            if (offset >= 0.5f) {
                y += (y1 > y0 ? 1 : -1);
                offset -= 1;
            }
        }
    }

    private static void fillPolygon(ObjModel model, List<PolygonPoint> polygon, BufferedImage image, double intensity, BufferedImage texture, double[][] zBuf) {
        if (polygon.size() != 3) {
            return;
        }

        int xMax = Integer.MIN_VALUE;
        int xMin = Integer.MAX_VALUE;
        int yMax = xMax;
        int yMin = xMin;

        for (PolygonPoint v : polygon) {
            int vInd = v.getVind();
            Vector point = model.getV(vInd);
            int x = getX(point.get(0));
            int y = getY(point.get(1));
            xMax = max(x, xMax);
            xMin = min(x, xMin);
            yMax = max(y, yMax);
            yMin = min(y, yMin);
        }

        int vInd = polygon.get(0).getVind();
        int vtInd = polygon.get(0).getVtInd();
        Vector point = model.getV(vInd);
        int x0 = getX(point.get(0));
        int y0 = getY(point.get(1));
        double z0 = point.get(2);
        double xt0 = model.getVt(vtInd).get(0);
        double yt0 = model.getVt(vtInd).get(1);

        vInd = polygon.get(1).getVind();
        vtInd = polygon.get(1).getVtInd();
        point = model.getV(vInd);
        int x1 = getX(point.get(0));
        int y1 = getY(point.get(1));
        double z1 = point.get(2);
        double xt1 = model.getVt(vtInd).get(0);
        double yt1 = model.getVt(vtInd).get(1);

        vInd = polygon.get(2).getVind();
        vtInd = polygon.get(2).getVtInd();
        point = model.getV(vInd);
        int x2 = getX(point.get(0));
        int y2 = getY(point.get(1));
        double z2 = point.get(2);
        double xt2 = model.getVt(vtInd).get(0);
        double yt2 = model.getVt(vtInd).get(1);

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                // Barycentric coordinates
                int l0Num = (y - y2) * (x1 - x2) - (x - x2) * (y1 - y2);
                int l0Den = (y0 - y2) * (x1 - x2) - (x0 - x2) * (y1 - y2);
                int l1Num = (y - y0) * (x2 - x0) - (x - x0) * (y2 - y0);
                int l1Den = (y1 - y0) * (x2 - x0) - (x1 - x0) * (y2 - y0);
                int l2Num = (y - y1) * (x0 - x1) - (x - x1) * (y0 - y1);
                int l2Den = (y2 - y1) * (x0 - x1) - (x2 - x1) * (y0 - y1);
                if (l0Num * l0Den < 0 || l1Num * l1Den < 0 || l2Num * l2Den < 0) {
                    continue;
                }
                double z = 1.0 * l0Num / l0Den * z0 +
                        1.0 * l1Num / l1Den * z1 +
                        1.0 * l2Num / l2Den * z2;

                if (z > zBuf[x][y]) {
                    zBuf[x][y] = z;
                    double textX = xt0 * l0Num / l0Den +
                            xt1 * l1Num / l1Den +
                            xt2 * l2Num / l2Den;

                    double textY = yt0 * l0Num / l0Den +
                            yt1 * l1Num / l1Den +
                            yt2 * l2Num / l2Den;

                    int textPixelX = (int) (textX * texture.getWidth());
                    int textPixelY = (int) (textY * texture.getHeight());
                    Color primColor = new Color(texture.getRGB(textPixelX, textPixelY));
                    Color colorToSet = new Color((int) (primColor.getRed() * intensity),
                            (int) (primColor.getGreen() * intensity),
                            (int) (primColor.getBlue() * intensity));
                    setPixel(x, y, image, colorToSet.getRGB());
                }
            }
        }
    }

    public static void render(ObjModel model, BufferedImage texture, BufferedImage image) {
        double[][] zBuf = new double[IMAGE_HEIGHT][IMAGE_WIDTH];
        for (int i = 0; i < zBuf.length; i++) {
            Arrays.fill(zBuf[i], -Double.MAX_VALUE);
        }
        System.out.println(zBuf[0][0]);
        for (Face f : model.getFaces()) {
            List<PolygonPoint> polygon = f.getPolygonPoints();

            Vector n = cross(sub(model.getV(polygon.get(2).getVind()), model.getV(polygon.get(0).getVind())), sub(model.getV(polygon.get(1).getVind()), model.getV(polygon.get(0).getVind())));
            n.normalize();
            Vector light = new Vector(0, 0, -1);
            double intensity = scalar(n, light);
            if (intensity <= 0) {
                continue;
            }

            fillPolygon(model, polygon, image, intensity, texture, zBuf);
        }
    }
}
