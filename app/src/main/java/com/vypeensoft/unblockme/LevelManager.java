package com.vypeensoft.unblockme;

import android.graphics.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class LevelManager {
    public static List<String> getPacks() {
        List<String> packs = new ArrayList<>();
        File baseDir = new File("/sdcard/Vypeensoft/Unblock_Me/game/levels/");
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] dirs = baseDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File d : dirs) {
                    packs.add(d.getName());
                }
            }
        }
        return packs;
    }

    public static List<Level> getLevels() {
        return getLevels("beginner");
    }

    public static List<Level> getLevels(String difficulty) {
        List<Level> levels = new ArrayList<>();
        File dir = new File("/sdcard/Vypeensoft/Unblock_Me/game/levels/" + difficulty);
        if (!dir.exists() || !dir.isDirectory()) {
            return levels; // Returns empty list if dir doesn't exist
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json") || name.toLowerCase().endsWith(".xml"));
        if (files == null) return levels;

        for (File file : files) {
            try {
                if (file.getName().toLowerCase().endsWith(".json")) {
                    InputStream is = new FileInputStream(file);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    String jsonStr = new String(buffer, "UTF-8");

                    JSONObject json = new JSONObject(jsonStr);
                    int id = json.getInt("id");
                    Level level = new Level(id);
                    if (json.has("difficulty")) {
                        level.difficulty = json.getString("difficulty");
                    }
                    if (json.has("minimumMoves")) {
                        level.minimumMoves = json.getInt("minimumMoves");
                    }

                    JSONArray cars = json.getJSONArray("cars");
                    for (int i = 0; i < cars.length(); i++) {
                        JSONObject car = cars.getJSONObject(i);
                        String carId = car.getString("id");
                        int x = car.getInt("x");
                        int y = car.getInt("y");
                        int length = car.getInt("length");
                        boolean horizontal = car.getBoolean("horizontal");
                        
                        boolean isTarget = carId.equals("X") || carId.equals("@");
                        int color = isTarget ? Color.RED : getColorForId(carId);

                        level.addBlock(new Block(carId.equals("@") ? "X" : carId, x, y, length, horizontal, color, isTarget, false));
                    }
                    levels.add(level);
                } else if (file.getName().toLowerCase().endsWith(".xml")) {
                    int id = 0;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(file.getName());
                    if (m.find()) {
                        try {
                            id = Integer.parseInt(m.group());
                        } catch (NumberFormatException e) {
                            id = file.getName().hashCode();
                        }
                    } else {
                        id = file.getName().hashCode();
                    }
                    Level xmlLevel = parseXmlLevel(file, id);
                    if (xmlLevel != null) {
                        levels.add(xmlLevel);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        Collections.sort(levels, new Comparator<Level>() {
            @Override
            public int compare(Level l1, Level l2) {
                return Integer.compare(l1.levelNumber, l2.levelNumber);
            }
        });

        return levels;
    }
    
    private static Level parseXmlLevel(File file, int id) {
        try {
            Level level = new Level(id);
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(file);

            org.w3c.dom.Element root = doc.getDocumentElement();
            
            org.w3c.dom.NodeList conditionNodes = doc.getElementsByTagName("resource");
            for (int i = 0; i < conditionNodes.getLength(); i++) {
                org.w3c.dom.Element res = (org.w3c.dom.Element) conditionNodes.item(i);
                if ("moves".equals(res.getAttribute("name"))) {
                    try {
                        level.minimumMoves = Integer.parseInt(res.getTextContent().trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            org.w3c.dom.NodeList lineNodes = doc.getElementsByTagName("line");
            String[][] grid = new String[6][6];
            for (int i = 0; i < lineNodes.getLength() && i < 6; i++) {
                org.w3c.dom.Element lineElem = (org.w3c.dom.Element) lineNodes.item(i);
                String[] parts = lineElem.getTextContent().trim().split(",");
                for (int j = 0; j < parts.length && j < 6; j++) {
                    grid[i][j] = parts[j].trim();
                }
            }

            boolean[][] processedGrid = new boolean[6][6];
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 6; x++) {
                    String cell = grid[y][x];
                    if (cell != null && !cell.equals("0") && !processedGrid[y][x]) {
                        
                        int length = 1;
                        boolean horizontal = true;
                        
                        for (int k = x + 1; k < 6; k++) {
                            if (cell.equals(grid[y][k]) && !processedGrid[y][k]) {
                                length++;
                            } else {
                                break;
                            }
                        }
                        
                        if (length == 1) {
                            for (int k = y + 1; k < 6; k++) {
                                if (cell.equals(grid[k][x]) && !processedGrid[k][x]) {
                                    length++;
                                } else {
                                    break;
                                }
                            }
                            if (length > 1) {
                                horizontal = false;
                            }
                        }
                        
                        if (horizontal) {
                            for (int k = 0; k < length; k++) {
                                processedGrid[y][x + k] = true;
                            }
                        } else {
                            for (int k = 0; k < length; k++) {
                                processedGrid[y + k][x] = true;
                            }
                        }
                        
                        boolean isTarget = cell.equals("@");
                        boolean isObstacle = cell.equals("X");
                        int color = isTarget ? Color.RED : (isObstacle ? Color.parseColor("#4B3621") : getColorForId(cell));
                        String blockId = cell.equals("@") ? "X" : (isObstacle ? "X_" + x + "_" + y : cell);
                        
                        level.addBlock(new Block(blockId, x, y, length, horizontal, color, isTarget, isObstacle));
                    }
                }
            }
            return level;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static int getColorForId(String id) {
        if (id.equals("X")) return Color.RED;
        
        int hash = id.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);
        
        r = (r % 128) + 64;
        g = (g % 128) + 100; // a bit more green
        b = (b % 128) + 128; // a bit more blue
        
        return Color.rgb(r, g, b);
    }
}
